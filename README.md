# Nav Lights (NeoForge 1.21.1)

Aircraft lights for Create builds. Works on Create contraptions, does not need Create installed.

## Blocks
**Nav Light** (dyeable, redstone toggled)
- Place on the face you want it to point out of
- Right-click with any dye to recolor (16 colors)
- Right-click with an empty hand to toggle strobe (double flash every 2 seconds)
- Glow gets bigger with distance so it is visible from far away (up to 256 blocks, limited by your render distance)

**Landing Light** (not dyeable, redstone toggled)
- Place on the face you want it to point out of
- Visible cone about 35 blocks long and 13 wide at the far end
- Beam stops at the first solid block and leaves a light pool on it
- Lens flare is brightest when you look straight into it

Both give real block light while powered (nav 10, landing 15).

## Crafting
- Nav Light x2: glass pane on top, redstone lamp in the middle, iron ingot on the bottom (one column)
- Landing Light: iron ingots in the 4 corners, glass panes top/left/right of the center, glowstone in the center, redstone bottom middle

## Building with GitHub
1. Make a new repo, upload everything in this folder (keep the `.github` folder)
2. Go to the Actions tab, open the latest "Build mod" run
3. Download the `navlights-jar` artifact at the bottom, unzip, drop the jar in your mods folder

If the build says it cannot find the NeoForge version, change `neo_version` in gradle.properties to a newer 21.1.x.
