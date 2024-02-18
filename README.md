# CustomPortals
Rewrite to the latest MC version forked from https://github.com/incognitojam/CustomPortals

Create portals in your Minecraft server made from alternative materials to teleport your users to different worlds. Custom world materials can be specified in the configuration file.

## Configuration
Below each configuration option is explained in detail.

#### Portal search options
`portal-search-range: 64` This specifies the radius in which the plugin should search for portals at the target location, before deciding to create a new portal (as none are present for the user to teleport to). *This value should always be written as an integer, not a decimal.*

`portal-creation-range: 24` The radius in which the plugin should search for a space to create a portal at the destination. If no space is found in this range then no portal is created and the teleport is cancelled. *This value should always be written as an integer, not a decimal.*

#### Miscellaneous
`debug-mode: false` Enables extra debugging in the console. If something doesn't seem right in the plugin, please enable debug mode, perform the action and report the full console log to a developer.

#### Portal materials

```yaml
portal-materials:
    # Material used to construct a portal 
    # which returns the player to the overworld.
    world: "stone"
    
    # Material used to construct a portal
    # which will teleport players to the nether.
    # Note: It is recommended to keep the nether
    # portal material as obsidian to prevent
    # player confusion.
    world_nether: "obsidian"
    
    # Material used to construct a portal
    # which will teleport players to a custom
    # world: the "arcade".
    arcade: "orange_wool"
```

Materials should be specified with quotes surrounding them and written in the format `"name"`.

#### World Scale
```yaml
world-scale:
    world: 1.0
    world_nether: 8.0
    arcade: 2.0
```

You must provide a world scale for each world that a portal may be created for. If this isn't specified, the teleport will not work. Decimal values are accepted but not recommended (untested).

***TODO:*** Implement default world scale.

##### Example:
When teleporting from `world_nether` to `arcade`, the x and y coordinates will be converted to `universe coordinates` by diving by the `world-scale` constant for `world_nether` (`8.0`). To convert the `u coordiantes` to `world coordinates` for `arcade` they must be multiplied by the `world-scale` for `arcade` (`2.0`).

###### Pseudo-code:
```
sourceWorld = "world_nether";
sourceWorldScale = 8.0;
sourceCoords = (100, 64, 100);

destinationWorld = "arcade";
destinationWorldScale = 2.0;

universeCoords = sourceCoords / sourceWorldScale;
               = (100/8, 64, 100/8)

destinationCoords = universeCoords * destinationWorldScale;
                  = (100/8 * 2, 64, 100/8 * 2)
                  = (25, 64, 25)
```
#### Permissions
`customportals.create`: allows the user to create portals. Defaults to all operators.

`customportals.reset`: allows the user to reset all portals in the server. Defaults to all operators.

## Bugs/Planned Features
* Ensuring portals link properly and don't create unnecessary portals
* Creating platforms for portals in unsafe locations
* Custom details

Please open an issue thread here on GitHub or [contact me directly](mailto:jacob5257.dev@gmail.com) if you encounter unexpected behavior.

## Releases
Releases can be found [here on GitHub](https://github.com/jacob52571/CustomPortals/releases).
As of right now, the plugin is still in pre-release as I am still rewriting/upgrading and I hope to have a production-ready version soon.

## Credits
All source code from this plugin was originally developed by [IncognitoJam](https://github.com/incognitojam/CustomPortals).
This plugin was developed by IncognitoJam at the request of [Robotnik](https://www.spigotmc.org/members/robotnik.9977/) on the SpigotMC forums.
