# Dual Wheel Mount

An addon for **Create Aeronautics** (Offroad module) on **NeoForge 1.21.1**.

Adds the **Dual Wheel Mount**: one block that holds a tire on each side. Each tire behaves like it would on a normal
Offroad Wheel Mount, including the suspension, steering from side redstone signals, and brakes.

## How it works

- **Placing:** the wheels end up on your left and right.
- **Tires:** right-click either wheel side with a tire to mount it. Right-click with an empty hand to take it off.
- **Power:** feed a rotating shaft into the top or bottom. Both wheels are driven.
- **Free rolling:** unpowered wheels still roll with the vehicle, so a plane pushed by jet engines rolls on its own
  and the tires spin with it.
- **Steering:** redstone on the two sides next to the wheels, the same as the Offroad mount. Both wheels turn together.
- **Brakes:** redstone on the top, like the Offroad mount, so connect the drive shaft from the bottom if you want brakes.
- **Suspension strength:** use the value box on the side faces.
- **Crafting:** 2 Wheel Mounts + 1 Shaft.

## Requirements

- Minecraft 1.21.1, NeoForge 21.1.219+
- Create 6.0.10+, Create Aeronautics (Simulated, Offroad, Sable 2.x)

## Status

Early prototype. The physics code is adapted from Offroad's wheel mount and has not been tested in game yet.

## Credits and license

The per-wheel suspension and rolling logic is adapted from the Create Aeronautics / Simulated project's Offroad
module, which is MIT licensed (c) The Simulated Team / The Creators of Aeronautics. This mod does not include any of
that project's textures or models; it references them at runtime from the Offroad mod.
