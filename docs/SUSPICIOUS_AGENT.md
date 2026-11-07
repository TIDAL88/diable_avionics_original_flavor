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
level check or commission requirement. This opens SWP's existing Gulf briefing,
where the player can accept or decline the bounty. The SP permanently unlocks
the briefing for that campaign. Declining keeps access unlocked after saving and
loading, even with no SP remaining.

The agent is connected to SWP. Accepting the offer starts SWP's actual Gulf
mission; its fleet, reward, recovery rules, and completion are handled by SWP.
The agent disappears if IBB is disabled, the bounty is active or completed, or
the player has acquired the Gulf. Ships in storage count too. Until acceptance,
the regular IBB route can still offer the Gulf.

`DASuspiciousAgentBarEvent.java` handles the character, station, opening,
and SP confirmation. One instance is stored in campaign memory; it has no random
event expiry timer. `DAIBBGulfBarEvent.java` handles the SWP offer. Both are in
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

The dialogue is in `data/config/modFiles/suspicious_agent_dialogue.json`.
The old weighted creator and save migration are removed. This version does not
support saves from the earlier agent implementations.

SWP remains optional at runtime. To compile the source, include SWP's
`jars/SWP.jar` and Diable's `lib/Diable-helpers.jar` alongside the existing build
dependencies. PatchLib is not needed. After rebuilding `jars/Diable.jar`, restart
the game and visit a Diable bar on a save where the Gulf is still available.
