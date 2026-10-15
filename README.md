# Copycat Casing

A small addon for [Create](https://modrinth.com/mod/create) on **NeoForge 1.21.1**.

Encase a shaft in **any full block** (wool, stone, planks, glass, ...) the same way Create's copycats work.
The shaft keeps transmitting rotation as normal; only the casing's look (and sound, friction and blast
resistance) changes.

## Blocks

| Block | Notes |
|---|---|
| Copycat Encased Shaft | Full block. |
| Copycat Encased Shaft Slab | Bottom or top half, placed like a vanilla slab. The shaft sticks out of the open half. |

## Using it

- **Crafting:** Shaft + Copycat Panel (full block). 1 full block -> 2 slabs, 2 slabs -> 1 full block.
- **Encase:** right-click with a full block. It is consumed.
- **Remove:** sneak + right-click with a Wrench. You get the block back and the casing turns into a plain shaft.
- Breaking the casing (or blowing it up) also drops the block it was wearing. Creative players get nothing back.
- Grass, leaves and other biome-tinted blocks keep their biome colour.

Blocks with a block entity (chests, furnaces, ...) and non-full blocks (stairs, slabs, fences, ...) can't be used as a casing.

## Requirements

- Minecraft 1.21.1, NeoForge 21.1.219 or newer
- Create 6.0.10 or newer for 1.21.1

## Known limitations

- There is no double slab and no waterlogging.
- Behaviour on moving contraptions has not been tested thoroughly.
- Only the stored block's default state is used, so orientation of logs etc. is not copied.

## Building

```
gradlew build
```

The jar ends up in `build/libs/`. Set `create_version` in `gradle.properties` to a build listed on
https://maven.createmod.net/com/simibubi/create/create-1.21.1/ . `gradlew runClient` starts a development client.

## License

See `mod_license` in `gradle.properties` (currently *All Rights Reserved*).
