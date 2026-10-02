"""Builds the demonstration body: a muscular MakeHuman male (CC0 assets) with the default rig, in
the owner's chosen look (2026-10-02): a plain grey anatomy model with the muscles drawn as a
schematic (each muscle lighter, the grooves between them darker), and the worked muscles in red."""
import bpy, bmesh, math, os
from mathutils import Vector
from bl_ext.user_default.mpfb.services.humanservice import HumanService
from bl_ext.user_default.mpfb.services.targetservice import TargetService
from bl_ext.user_default.mpfb.services.locationservice import LocationService

HERE = os.path.dirname(__file__)
bpy.ops.wm.read_factory_settings(use_empty=True)

macros = TargetService.get_default_macro_info_dict()
macros.update(gender=1.0, muscle=1.0, weight=0.72, age=0.45, height=0.55, proportions=1.0)
body = HumanService.create_human(macro_detail_dict=macros, scale=0.1)
body.name = "Body"
rig = HumanService.add_builtin_rig(body, "default")
rig.name = "Rig"

# Rest-pose positions decide the muscle regions: MakeHuman faces -Y, Z is up.
mesh = body.data
groups = {g.name: g.index for g in body.vertex_groups}
def weight(v, *names):
    total = 0.0
    for e in v.groups:
        for n in names:
            if groups.get(n) == e.group:
                total += e.weight
    return min(total, 1.0)

# Body landmarks from the rest pose.
zs = [v.co.z for v in mesh.vertices]
top = max(zs)
def frac(v):
    return v.co.z / top

def smooth(edge0, edge1, x):
    t = max(0.0, min(1.0, (x - edge0) / (edge1 - edge0)))
    return t * t * (3 - 2 * t)

def regions(v):
    co = v.co
    x, y, z = abs(co.x), co.y, frac(v)
    front = smooth(0.0, 0.02, -y)  # in front of the body's mid-plane
    back = 1.0 - front
    both = lambda *n: weight(v, *[f"{m}.L" for m in n] + [f"{m}.R" for m in n])
    r = {}
    upperarm = both("upperarm01", "upperarm02")
    shoulder = both("shoulder01", "clavicle")
    r["shoulders"] = max(shoulder, both("upperarm01") * smooth(0.0, 1.0, 1.0)) * (1 if z > 0.72 else 0)
    r["biceps"] = upperarm * front
    r["triceps"] = upperarm * back
    r["forearms"] = both("lowerarm01", "lowerarm02")
    chestw = weight(v, "spine01", "breast.L", "breast.R")
    r["chest"] = chestw * front * smooth(0.68, 0.72, z) * (1 - smooth(0.8, 0.82, z))
    r["abdominals"] = weight(v, "spine02", "spine03", "spine04") * front * (1 - smooth(0.69, 0.71, z))
    spineback = weight(v, "spine01", "spine02", "spine03") * back
    r["lats"] = spineback * smooth(0.07, 0.11, x)
    r["middle back"] = weight(v, "spine01", "spine02") * back * (1 - smooth(0.07, 0.1, x))
    r["lower back"] = weight(v, "spine04", "spine05") * back * smooth(0.5, 0.55, z)
    r["traps"] = (weight(v, "neck01") * back + both("clavicle") * back) * smooth(0.78, 0.8, z)
    r["neck"] = weight(v, "neck01", "neck02", "neck03") * front
    r["glutes"] = both("pelvis") * back
    thigh = both("upperleg01", "upperleg02")
    r["quadriceps"] = thigh * front
    r["hamstrings"] = thigh * back * (1 - smooth(0.46, 0.5, z))
    r["adductors"] = thigh * (1 - smooth(0.05, 0.09, x)) * smooth(0.36, 0.42, z)
    r["abductors"] = (thigh + both("pelvis")) * smooth(0.12, 0.15, x) * smooth(0.45, 0.5, z)
    r["calves"] = both("lowerleg01", "lowerleg02") * back * smooth(0.12, 0.18, z)
    return {k: min(1.0, v) for k, v in r.items()}

MUSCLES = ["shoulders", "biceps", "triceps", "forearms", "chest", "abdominals", "lats", "middle back", "lower back",
           "traps", "neck", "glutes", "quadriceps", "hamstrings", "adductors", "abductors", "calves"]
values = {name: [0.0] * len(mesh.vertices) for name in MUSCLES}
for v in mesh.vertices:
    for name, value in regions(v).items():
        values[name][v.index] = value
# Adding an attribute invalidates earlier references, so create each and fill it at once.
for name in MUSCLES:
    mesh.attributes.new(name="m_" + name.replace(" ", "_"), type="FLOAT", domain="POINT")
    mesh.attributes["m_" + name.replace(" ", "_")].data.foreach_set("value", values[name])
# The attribute the shader reads, filled per exercise by the renderer: x = primary, y = secondary.
mesh.attributes.new(name="worked", type="FLOAT2", domain="POINT")

# The schematic: how much of each point lies inside some muscle, so grooves between muscles show.
definition = [max(values[name][i] for name in MUSCLES) for i in range(len(mesh.vertices))]
mesh.attributes.new(name="definition", type="FLOAT", domain="POINT")
mesh.attributes["definition"].data.foreach_set("value", definition)

# One grey material for the whole body: darker in the grooves, lighter on the muscles, red where
# worked (the renderer fills "worked": x = primary, y = secondary).
mat = bpy.data.materials.new("Anatomy")
mat.use_nodes = True
nodes, links = mat.node_tree.nodes, mat.node_tree.links
bsdf = nodes["Principled BSDF"]
bsdf.inputs["Roughness"].default_value = 0.55
defn = nodes.new("ShaderNodeAttribute"); defn.attribute_name = "definition"
ramp = nodes.new("ShaderNodeValToRGB")
ramp.color_ramp.elements[0].position = 0.25
ramp.color_ramp.elements[0].color = (0.22, 0.24, 0.25, 1)
ramp.color_ramp.elements[1].position = 0.75
ramp.color_ramp.elements[1].color = (0.52, 0.55, 0.56, 1)
links.new(defn.outputs["Fac"], ramp.inputs["Fac"])
worked = nodes.new("ShaderNodeAttribute"); worked.attribute_name = "worked"
sep = nodes.new("ShaderNodeSeparateXYZ")
links.new(worked.outputs["Vector"], sep.inputs[0])
light = nodes.new("ShaderNodeMix"); light.data_type = "RGBA"; light.inputs["B"].default_value = (0.85, 0.42, 0.42, 1)
red = nodes.new("ShaderNodeMix"); red.data_type = "RGBA"; red.inputs["B"].default_value = (0.78, 0.06, 0.07, 1)
links.new(ramp.outputs["Color"], light.inputs["A"])
links.new(sep.outputs["Y"], light.inputs["Factor"])
links.new(light.outputs["Result"], red.inputs["A"])
links.new(sep.outputs["X"], red.inputs["Factor"])
links.new(red.outputs["Result"], bsdf.inputs["Base Color"])
body.data.materials.clear()
body.data.materials.append(mat)

bpy.ops.wm.save_as_mainfile(filepath=os.path.join(HERE, "figure.blend"))
print("BUILT", body.name, rig.name, round(top, 3))
