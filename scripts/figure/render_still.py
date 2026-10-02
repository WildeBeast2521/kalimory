import bpy, math, os, sys
from mathutils import Vector
HERE = os.path.dirname(__file__)
args = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
primary = args[0].split(",") if args else []
secondary = args[1].split(",") if len(args) > 1 else []
out = args[2] if len(args) > 2 else os.path.join(HERE, "still.png")
yaw = float(args[3]) if len(args) > 3 else 35.0

bpy.ops.wm.open_mainfile(filepath=os.path.join(HERE, "figure.blend"))
body = bpy.data.objects["Body"]
mesh = body.data
n = len(mesh.vertices)
p = [0.0] * n
s2 = [0.0] * n
for name in primary:
    vals = [0.0] * n
    mesh.attributes["m_" + name.replace(" ", "_")].data.foreach_get("value", vals)
    p = [max(a, b) for a, b in zip(p, vals)]
for name in secondary:
    vals = [0.0] * n
    mesh.attributes["m_" + name.replace(" ", "_")].data.foreach_get("value", vals)
    s2 = [max(a, b) for a, b in zip(s2, vals)]
flat = []
for a, b in zip(p, s2):
    flat += [a, b]
mesh.attributes["worked"].data.foreach_set("vector", flat)

scene = bpy.context.scene
scene.render.engine = "BLENDER_EEVEE_NEXT"
scene.render.resolution_x = scene.render.resolution_y = 512
scene.render.film_transparent = True
cam_data = bpy.data.cameras.new("cam"); cam_data.type = "ORTHO"; cam_data.ortho_scale = 2.1
cam = bpy.data.objects.new("cam", cam_data); scene.collection.objects.link(cam)
t = math.radians(yaw); d = 6
cam.location = Vector((d * math.sin(t), -d * math.cos(t), 0.9 + d * math.tan(math.radians(12))))
cam.rotation_euler = (Vector((0, 0, 0.9)) - cam.location).to_track_quat("-Z", "Y").to_euler()
scene.camera = cam
sun = bpy.data.lights.new("sun", "SUN"); sun.energy = 3.0
so = bpy.data.objects.new("sun", sun); so.rotation_euler = (math.radians(40), math.radians(10), math.radians(25)); scene.collection.objects.link(so)
fill = bpy.data.lights.new("fill", "SUN"); fill.energy = 1.0
fo = bpy.data.objects.new("fill", fill); fo.rotation_euler = (math.radians(70), 0, math.radians(-120)); scene.collection.objects.link(fo)
world = bpy.data.worlds.new("w"); world.use_nodes = True
world.node_tree.nodes["Background"].inputs[1].default_value = 0.5
scene.world = world
scene.render.filepath = out
bpy.ops.render.render(write_still=True)
print("RENDERED", out)
