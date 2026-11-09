# Building site settings

Any building can declare named choices under `userConfig`. The existing building
details dialog exposes them through **Building settings**. Each site stores its
selection independently in its existing YAML/database state, under
`userConfig.<setting ID>`. No migration is needed for existing sites.

```yaml
userConfig:
  product:
    displayName: factions.building.settings.ore
    default: none
    options:
      none:
        displayName: factions.building.settings.off
      iron:
        displayName: factions.economy.resource.iron
        requiredBuildings:
          geologists_house: 1
        effects:
          ResourceProduction:
            production:
              iron: 370
```

- Labels accept translation keys, with the literal label as fallback. Add custom
  keys to both server language files (their `factions.` prefix is implicit).
- Setting and option IDs accept letters, digits, underscores and hyphens. Quote
  YAML boolean-like IDs such as `off`; prefer `none` for an empty choice.
- `default` must identify an option; if omitted, the first option is the default.
  Missing or removed saved options fall back to that default.
- `requiredBuildings` counts finished, active buildings in the owning faction.
  It gates selecting the option. It is an unlock check, not an ongoing production
  dependency: removing the prerequisite later does not clear a saved choice.
- Choose an unrestricted default, such as `none`, when other options are locked.
  Default effects are resolved on load without testing unlock requirements.
- Each option accepts the same `effects` format as the building's base effects,
  including suffixes such as `ResourceProduction_ore` and ticking item effects.
- Effects are merged by their full YAML key. A choice replaces a base effect
  with the same key. Otherwise it adds an effect. Later settings override earlier
  settings on key collisions. Use distinct keys for independent settings.
- An empty option adds no effects; it does not remove base effects. Put switchable
  production entirely inside the options when `none` should stop production.
- Block modifiers from all choices are included in the building's block scan.

Changes require the existing faction privileged-member permission and a finished,
active site that is neither destroyed nor upgrading. Click callbacks recheck the
permission, site membership and prerequisites. Visitors may inspect current settings.

The change stages new effects and saves the site before replacing the old effects.
Instantiation/save failures retain the old selection and effects. Unchanged effects
keep their existing instances and timers. Removed effects leave both faction effect
registries, including ticking effects. Existing chest contents and buffers are kept.

## Verification

`BuildingUserConfigTest` covers independent site choices through YAML serialization,
fallback for removed choices, and rejection of an invalid default. Runtime checks:

1. Open two completed sites of the same building; change only one site's choice.
2. Verify the other site's choice and production remain unchanged.
3. Switch between production options and `none`; check only the current output is
   produced, with no duplicate ticking or payday effects.
4. Restart and verify the selection and output are restored.
5. Try a locked choice, a non-privileged member and a dialog opened before losing
   privileges. None should change the setting.
6. For physical-item options, confirm buffered items survive switching and chests
   are available when the new effect requires them.

The mine example used in the Erethon dev config is data only. No mine, ore, or
geologist identifier is hardcoded in the Java implementation.
