
# License & Credits

## License & Asset Reuse

This project is released under a permissive license in accordance with the original Diable Avionics distribution terms. 

### Reuse Terms
Any work contained in this mod is free to be reused, modified, or adapted for other **Starsector mods**, subject to the following author credit requirements:

* **Original Diable Avionics Assets & Code:** Free to reuse or kitbash provided Tartiflette and FlashFrozen are credited and that the mod follows Tartiflette's license.
* **TIDAL's Additions & Code:** Free to reuse or adapt provided TIDAL is credited as the author.
* **Aero's Additions & Code:** Free to reuse or adapt provided Aero is credited as the author.
* **Third-Party Contributor Assets:** Free to reuse in accordance with the original creators' permissions listed below, provided appropriate attribution is given to the respective creators.

---

## Credits & Attribution

* **Tartiflette, FlashFrozen, & Debido:** Original Diable Avionics mod creation, assets, and codebase.
* **After/Aero:** Balance, art/writing/concepts, Quest/environment development, AI design(Grazer)
* **TIDAL:** Balance, Code QC, AI implementation (Grazer/Assume Control), Gulf hullmods
<br>

* **Spectros:** Playtesting.
* **Tranquility:** Fleetbuilder tweaks and code.
* **Zone51:** Calm AI code and initial Coanda sprite modification.
* **Nia Tahl:** Paintjobs.
* **Selkie:** 4-large-mount Pandemonium sprite variant and paintjobs.
* **Crablobab:** Jump code application.

## Building in IntelliJ IDEA

Local `.idea` and `.iml` files are ignored by Git, so a new checkout or recreated
module needs its build settings restored manually.

- Mark only `src` and `data/missions` as source folders. Keep the project root as
  a content root, without marking it as a source or resource folder. Otherwise
  graphics, sounds, and IDE backups are copied into the output JAR.
- Attach the Starsector and mod libraries to the module with Compile scope.
  Use `lib/Diable-helpers.jar` for the local utility dependency, rather than the
  generated `jars/Diable.jar`. See [helper provenance](lib/README.md).
- Configure the Diable JAR artifact to contain the module output plus the
  extracted contents of `lib/Diable-helpers.jar`, with output in `jars`.
- Use Build → Rebuild Project after correcting source roots to remove stale
  resources from the compiler output.
