# Suspicious Diable bar agent

Dialogue: `data/config/modFiles/suspicious_agent_dialogue.json`.

Event: `src/data/scripts/campaign/bar/DASuspiciousAgentBarEvent.java`.

`DAModPlugin.onGameLoad()` registers one persistent event in `PortsideBarData`.
It is available at every Diable-owned bar, including below level 6, and does not
use a random event creator or a global encounter cooldown. The same male agent
and portrait persist across markets and save/load. The portrait is a vanilla
asset and can be replaced in `getPersonPortrait()`.

The opening leads to a story option requiring level 6 and 1 SP, with no commission
or reputation requirement. The native story-option confirmation handles spending
and grants 100% bonus XP, matching SWP's alternate-bounty option. Leaving or
cancelling the confirmation spends nothing.

## TIDAL's integration points

All IBB logic is deliberately absent. There is no SWP or PatchLib dependency,
reflection, mission creation, tracker mutation, or replacement Gulf encounter.

- `shouldShowAgent()` currently returns `true`. Add the checks for IBB being
  enabled, the Gulf contract's state, and any other desired appearance conditions.
- `isBountyAvailable()` currently returns `false`. Fill this in when the handoff
  is implemented. The paid option stays greyed out until then, preventing SP
  spending on an empty action. The level and SP checks are already wired.
- `showGulfBounty()` is the empty handoff, called once per conversation after the
  story-point confirmation. Use the inherited `dialog`, `memoryMap`, `market`, and
  `person` fields to enter SWP's Gulf offer. A leave option is supplied as a
  fallback; replace the options when presenting the briefing. Do not charge SP
  again. Eligibility is rechecked at confirmation; the remaining SP balance is
  not, because the native UI may already have deducted the cost.

The briefing, acceptance, refusal, rewards, and completion belong to SWP:

- `data.scripts.campaign.intel.SWP_IBBIntel`
- `SWP_IBBIntel.FamousBountyStage.STAGE_GULF`
- `data.scripts.campaign.intel.SWP_IBBTracker`
- `data.scripts.campaign.intel.bar.events.SWP_IBBBarEvent`

Outstanding offers and preventing duplicate acceptance through the two routes
are part of that integration. No bounty eligibility is assumed by this scaffold.
