# Gulf bounty bar contact

Players normally have to wait for the Gulf bounty to come up through SWP's IBB
agents. That can take a while, and those agents don't normally appear in Diable
bars. This character gives players a way to ask for that specific bounty.

From level 5, he has a 30% discovery chance at each Diable-owned market. The rule
uses vanilla `RollProbability 0.3`, which keeps the result fixed for the same
market and month. Reopening the bar doesn't reroll it; another market or month
provides another opportunity. This is separate from the weighted event pool, so
other mods' event weights don't reduce his chance. A successful roll puts his
option first among the bar's rule-based events so it gets a visible slot.

Seeing his bar option doesn't fix his location. Once the player approaches him,
he stays at that market and stops appearing elsewhere. He stays even if the
player leaves without paying. The closing line tells the player they can return;
its impatience is flavor, with no deadline on the contact.

The player spends 1 SP on "Let's hear about this contract." There is no further
level check or commission requirement. This opens the selected contract's briefing,
where the player can accept or decline the bounty. The SP permanently unlocks
the briefing for that campaign. Declining keeps access unlocked after saving and
loading, even with no SP remaining.

With IBB enabled, accepting starts SWP's actual Gulf mission. Its fleet, reward,
recovery rules, and completion are handled by SWP. Until acceptance, the regular
IBB route can still offer the Gulf.

Without SWP, or with IBB disabled, he offers the existing MagicBounty "A cold
plate of revenge." Its own requirements still apply: currently +50 Diable
reputation and 150 fleet points. The contract option stays disabled until those
requirements are met; approaching him still fixes his station. The fallback has
its own opening about the Gulf in Tri-Tachyon hands. It displays the existing
bounty text, reward, reputation, deadline and map marker before acceptance.

The fallback uses MagicLib's public API to attach to the existing `$gulf` fleet
and accept `diable_gulf`. It doesn't create another fleet or replace the mission.
The regular board and this agent use the same contract. Fleet balancing remains
in `DiableavionicsGen.spawnGulf()` and the Gulf Boss variant.

Either route hides the agent when its bounty is active, completed or otherwise
closed, or the player has acquired the Gulf. Ships in storage count too. An
unavailable IBB stage doesn't switch to the standalone contract while IBB is on.

`DASuspiciousAgentBarEvent.java` handles the character, station, opening,
and SP confirmation. One instance is stored in campaign memory; it has no random
event expiry timer. `DAIBBGulfBarEvent.java` handles the SWP offer;
`DAMagicGulfBarEvent.java` handles the MagicBounty offer. `DAGulfBountyState.java`
contains the shared acquired-ship checks and route selection. These are in
`src/data/scripts/campaign/bar/`.

`data/campaign/rules.csv` contains the discovery roll, the return visit rule, and
the approach option's handoff. `src/data/campaign/rulecmd/DASuspiciousAgentCMD.java`
adds the bar entry and opens the existing conversation through vanilla's
`BarEventDialogPlugin`, which returns to the bar when the conversation ends.

SWP's `SWP_IBBIntel.init()` chooses its own target. The Gulf offer uses the existing
`data.scripts.util.ReflectionUtils` to set `STAGE_GULF`, the captain, payment, and
target description before opening SWP's briefing. These fields are prepared
before the player can spend SP. The description matches SWP 1.16.6; check the
initializer fields and text if updating to another SWP version.

The offer uses `SWP_IBBTracker` for availability and checks again at acceptance.
After accepting through this agent, any older regular IBB Gulf offer is removed.
Other IBB targets keep their existing progress. This contact has no random-target
reroll option.

The MagicBounty adapter uses no reflection. Eligibility is rechecked before
spending SP and accepting, and its prepared contract must still be the same
unaccepted object held by MagicLib's coordinator.

Checks run on bar and dialogue actions. Level and station checks happen first.
Once MagicLib has prepared the contract, the adapter validates its stored fleet
in its current location rather than searching every system again. Payment and
acceptance still recheck live eligibility.

The dialogue is in `data/config/modFiles/suspicious_agent_dialogue.json`.
The old weighted creator and save migration are removed. This version does not
support saves from the earlier agent implementations.

SWP remains optional at runtime. To compile the source, include SWP's
`jars/SWP.jar` and Diable's `lib/Diable-helpers.jar` alongside the existing build
dependencies. PatchLib is not needed. After rebuilding `jars/Diable.jar`, restart
the game and visit a Diable bar on a save where the Gulf is still available.
