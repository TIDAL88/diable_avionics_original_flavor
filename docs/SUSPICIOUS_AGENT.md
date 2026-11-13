# Gulf bounty bar contact

Players normally have to wait for the Gulf bounty to come up through SWP's IBB
agents. That can take a while, and those agents don't normally appear in Diable
bars. This character gives players a way to ask for that specific bounty.

From level 6, he has a 30% discovery chance at each Diable-owned market while the
player meets the selected bounty's requirements. The rule
uses vanilla `RollProbability 0.3`, which keeps the result fixed for the same
market and month. Reopening the bar doesn't reroll it; another market or month
provides another opportunity. This is separate from the weighted event pool, so
other mods' event weights don't reduce his chance. A successful roll puts his
option first among the bar's rule-based events so it gets a visible slot.

Seeing his bar option doesn't fix his location. Once the player approaches him,
he stays at that market and stops appearing elsewhere. He stays even if the
player leaves without taking the contract. If eligibility drops, he is hidden
until it is restored, then returns at the same market. The closing line tells the
player they can return; its impatience is flavor, with no deadline on the contact.

The IBB contract costs 1 SP on "I'll take the contract." Confirmation immediately
accepts it through SWP's existing acceptance code. Payment is remembered, so an
expired IBB contract doesn't charge another SP. There is no second accept/decline
step or commission requirement.

With IBB enabled, accepting starts SWP's actual Gulf mission. Its fleet, reward,
recovery rules, and completion are handled by SWP. Until acceptance, the regular
IBB route can still offer the Gulf.

Without SWP, or with IBB disabled, he offers the existing MagicBounty "A cold
plate of revenge." This is free and requires level 6 and Favorable reputation
(+10) or better with Diable Avionics, with no minimum fleet size. The same checks
apply to appearance and acceptance. No story-point confirmation, payment, or paid
access flag is used for this route. Taking it immediately accepts the bounty and
the agent hands over a private access key. "Open the restricted posting" displays
the briefing, reward and map marker without accepting again. The fallback has its
own opening about the Gulf in Tri-Tachyon hands. There is no deadline.
The fleet uses the same NPC avoidance flags as MagicLib's generated
bounties, applied at spawn and through `fleet_memory_flags` on acceptance.

The fallback uses MagicLib's public API to attach to the existing `$gulf` fleet
and accept `diable_gulf`. It doesn't create another fleet or replace the mission.
The regular board and this agent use the same contract. `DiableGulfBountyFleetFactory`
builds the standalone fleet; `DiableavionicsGen.spawnGulf()` places it in the sector.
The escort uses 245 combat FP, zero logistics FP, quality 1.5, and an average
S-mod budget of 2. Anna is level 10 with 5 elite skills and 2 fleet skills from
IBB's skill pool. Up to 11 escorts get level 7 officers with 3 elite skills each.
These match the Gulf IBB stage's parameters, using Tri-Tachyon's ship roster.
The Gulf's Boss variant stays unchanged and is added outside the escort budget.

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

The MagicBounty adapter uses no reflection or story points. Eligibility is checked
before the contact appears and again before acceptance. Its prepared contract must
still be the same unaccepted object held by MagicLib's coordinator.

Checks run on bar and dialogue actions. Level and station checks happen first.
Once MagicLib has prepared the contract, the adapter validates its stored fleet
in its current location rather than searching every system again. Payment and
acceptance still recheck live eligibility.

The dialogue is in `data/config/modFiles/suspicious_agent_dialogue.json`.

SWP remains optional at runtime. To compile the source, include SWP's
`jars/SWP.jar` and Diable's `lib/Diable-helpers.jar` alongside the existing build
dependencies. PatchLib is not needed. After rebuilding `jars/Diable.jar`, restart
the game and visit a Diable bar on a save where the Gulf is still available.
