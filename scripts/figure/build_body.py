"""Builds the demonstration body: a muscular MakeHuman male (CC0 assets) with the default rig,
plain shorts cut from the body itself, and one colour attribute per catalogue muscle."""
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
skin = os.path.join(LocationService.get_user_data("skins"), "young_caucasian_male", "young_caucasian_male.mhmat")
HumanService.set_character_skin(skin, body, skin_type="MAKESKIN")
brow = os.path.join(LocationService.get_user_data("eyebrows"), "eyebrow001", "eyebrow001.mhclo")
if os.path.exists(brow):
    HumanService.add_mhclo_asset(brow, body, asset_type="Eyebrows", subdiv_levels=0)
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

# Shorts: the body's faces from the waist to mid-thigh, pushed out a little, in a dark fabric.
bpy.ops.object.select_all(action="DESELECT")
body.select_set(True)
bpy.context.view_layer.objects.active = body
bpy.ops.object.duplicate()
shorts = bpy.context.active_object
shorts.name = "Shorts"
for m in list(shorts.modifiers):
    if m.type != "ARMATURE":
        shorts.modifiers.remove(m)
bm = bmesh.new()
bm.from_mesh(shorts.data)
waist = 0.555 * top
hem = 0.40 * top
# Only real skin: MakeHuman's helper geometry (tights, skirt) is not in the "body" group.
deform = bm.verts.layers.deform.active
body_index = groups["body"]
remove = [v for v in bm.verts if not (hem < v.co.z < waist) or abs(v.co.x) > 0.25 or body_index not in v[deform]]
bmesh.ops.delete(bm, geom=remove, context="VERTS")
for v in bm.verts:
    v.co += v.normal * 0.008
bm.to_mesh(shorts.data)
bm.free()
mat = bpy.data.materials.new("Shorts")
mat.use_nodes = True
mat.node_tree.nodes["Principled BSDF"].inputs["Base Color"].default_value = (0.05, 0.07, 0.08, 1)
mat.node_tree.nodes["Principled BSDF"].inputs["Roughness"].default_value = 0.8
shorts.data.materials.clear()
shorts.data.materials.append(mat)
solid = shorts.modifiers.new("thick", "SOLIDIFY")
solid.thickness = 0.004

# Skin shader: mix towards red where worked.
for m in body.data.materials:
    if not m or not m.use_nodes:
        continue
    nodes, links = m.node_tree.nodes, m.node_tree.links
    bsdf = next((n for n in nodes if n.type == "BSDF_PRINCIPLED"), None)
    if bsdf is None:
        continue
    color_in = bsdf.inputs["Base Color"]
    source = color_in.links[0].from_socket if color_in.links else None
    attr = nodes.new("ShaderNodeAttribute"); attr.attribute_name = "worked"
    sep = nodes.new("ShaderNodeSeparateXYZ")
    links.new(attr.outputs["Vector"], sep.inputs[0])
    light = nodes.new("ShaderNodeMix"); light.data_type = "RGBA"; light.inputs["B"].default_value = (0.85, 0.35, 0.35, 1)
    red = nodes.new("ShaderNodeMix"); red.data_type = "RGBA"; red.inputs["B"].default_value = (0.75, 0.05, 0.06, 1)
    if source is not None:
        links.new(source, light.inputs["A"])
    else:
        light.inputs["A"].default_value = color_in.default_value
    links.new(sep.outputs["Y"], light.inputs["Factor"])
    links.new(light.outputs["Result"], red.inputs["A"])
    links.new(sep.outputs["X"], red.inputs["Factor"])
    links.new(red.outputs["Result"], color_in)

bpy.ops.wm.save_as_mainfile(filepath=os.path.join(HERE, "figure.blend"))
print("BUILT", body.name, rig.name, shorts.name, round(top, 3))
