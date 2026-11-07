# Gulf bounty bar contact

Players normally have to wait for the Gulf bounty to come up through SWP's IBB
agents. That can take a while, and those agents don't normally appear in Diable
bars. This character gives players a way to ask for that specific bounty.

He appears in every Diable-owned bar. The player talks to him, then spends 1 SP
on "Let's hear about this contract." That option requires level 6, with no
commission requirement. It should open SWP's existing Gulf briefing, where the
player can accept or decline the bounty. The SP pays for access to the briefing,
so declining afterwards does not refund it.

The character, opening dialogue, level requirement, and SP confirmation are done.
The connection to SWP still needs writing. For now, the contract option is greyed
out so it cannot charge the player for an unfinished action.

The awkward part is selecting the Gulf: `SWP_IBBIntel.init()` chooses a bounty
itself and has no argument for requesting a particular one. The connection needs
to select `FamousBountyStage.STAGE_GULF` and initialize that mission correctly.
SWP already has the briefing, acceptance/refusal dialogue, fleet, and rewards.

Both ways of getting the bounty need to use SWP's progress tracking. If the player
accepts it from either agent, the other must stop offering it. Check old bar offers
too, so returning to an earlier conversation cannot create a second Gulf fleet.
Hide this character when IBB is disabled, the bounty is active or completed, or
the player has already acquired the Gulf. Until the player accepts, the normal
IBB route can still offer it.

The methods left for this work are in
`src/data/scripts/campaign/bar/DASuspiciousAgentBarEvent.java`:

- `shouldShowAgent()`: decide whether the character should appear. Currently
  always returns `true`.
- `isBountyAvailable()`: decide whether to enable the contract option. Currently
  always returns `false`.
- `showGulfBounty()`: open SWP's Gulf briefing. Currently empty. The SP confirmation
  calls this method; payment is already handled. `dialog`, `memoryMap`, `market`,
  and `person` are available in the class.

The SWP classes to look at are `data.scripts.campaign.intel.SWP_IBBIntel`,
`data.scripts.campaign.intel.SWP_IBBTracker`, and
`data.scripts.campaign.intel.bar.events.SWP_IBBBarEvent`.

The dialogue is in `data/config/modFiles/suspicious_agent_dialogue.json`.
Registration is already in `DAModPlugin.onGameLoad()`.
