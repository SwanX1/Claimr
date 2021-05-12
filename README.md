<h1 align="center">Claimr<br>a server-side claiming mod</h1>

**Idea inspired by FTB Chunks**
**This mod is server-side**

**By contributing to this project, you agree to release your work into the public domain.**

### Data format
This mod saves data for this mod in `world/data/claimr.json`, under the following format:

```json
{
  "chunks": {
    "dimension;chunk_x:chunk_z": "group_name"
  },
  "groups": {
    "group_name": {
      "personal": boolean,
      "owner": uuid,
      "members": {
        "uuid": integer
      }
    }
  }
}
```

<code>chunks</code> contains chunk claim data.<br>
<code>groups</code> contains group data.<br>
<code>group_name</code> contains information if the group is an individual or multiple players.<br>
<code>group_name</code> will be a user-selected group name, or the UUID of the owner if the group is personal.<br>
<code>members</code> contains a UUID with a rank level:<br>
  0 - No affiliation (shouldn't be present in JSON file, used internally)<br>
  1 - Can interact within the claim<br>
  2 - Can manage members (groups only)<br>
  3 - Is the owner (groups only)<br>

### Commands

`>` - Redirects to another command
`-` - Description of command

```
/claimr
  > /claimr help

/claimr help
  - Shows help for this mod.

/claimr info [<debug>]
  - Shows info about the mod
    <debug> - boolean
      Shows additional info,
      needs permission level 3 and above.
      Defaults to false

/claimr ci
  > /claiminfo

/claimr claiminfo
  > /claiminfo

/claimr claim
  > /claim

/claimr unclaim
  > /unclaim

/claimr unclaimall
  > /unclaimall

/claimr trust
  > /trust

/claimr untrust
  > /untrust

/claimr listtrusted
  > /listtrusted

/claimr group
  > /group

/claiminfo             
  - Shows info about the chunk at your posisition.
    Example response:
      Chunk Location: minecraft:overworld;0:1
      This chunk is unclaimed
   
    Example response:
      Chunk Location: minecraft:overworld;6:-9
      Claimed by: SwanX1

/ci
  > /claiminfo

/claim [<group>]
  - Claims the current chunk,
    throws error if you aren't a manager of the group
    or if the chunk is already claimed.
    <group> - string;
      Determines the group you are claiming this chunk for.
      Defaults to player uuid

/unclaim [<group>]
- Unclaims the current chunk, 
  throws error if you aren't a manager of the group
  or if the chunk is not claimed by the group.
  <group> - string;
    Determines the group you are unclaiming this chunk from.
    Defaults to player uuid

/unclaimall [<group>]
- Unclaims all chunks claimed by the group,
  throws error if you aren't a manager of the group.
  <group> - string;
  Determines the group you are unclaiming this chunk from.
  Defaults to player uuid

/trust <...players>
- Adds players as interactors to your personal claim.

/untrust <...players>
- Removes players as interactors from your personal claim.

/listtrusted
- Lists interactors of your personal claim.

/group create <group>
- Creates a group,
  throws error if group already exists.
```