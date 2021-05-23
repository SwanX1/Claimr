<h1 align="center">Claimr<br>a server-side claiming mod</h1>

**Idea inspired by FTB Chunks**<br>
**This mod is server-side**

**By contributing to this project, you agree to release your work for this project into the public domain.**

### Data format
This mod saves data for this mod in `world/data/claimr.json`, under the following format:

```json5
{
  "chunks": {
    "dimension;chunk_x:chunk_z": "group_name"
  },
  "groups": {
    "group_name": {
      "owner": "0228224b-0143-489d-8ff3-60332602eb87", // UUID
      "members": {
        "1fbfc08c-1e1f-4124-a56b-e4f14dae2319": 1 // Rank
      }
    }
  }
}
```

<code>chunks</code> contains chunk claim data.<br>
<code>groups</code> contains group data.<br>
<code>group_name</code> will be a user-selected group name.<br>
<code>owner</code> contains the UUID of the owner of the group<br>
<code>members</code> contains a UUID with a rank level:<br>
  0 - No affiliation (shouldn't be present in JSON file, used internally)<br>
  1 - Can interact within the claim<br>
  2 - Can manage members<br>
  3 - Is the owner (shouldn't be present in JSON file, owner is set in the <code>owner</code> field)<br>

### Commands

<code>&gt;</code> - Redirects to another command<br>
<code>-</code> - Description of command

<pre>
/claimr
  > /claimr help

/claimr help
  - Shows help for this mod.

/claimr info [&lt;debug&gt;]
  - Shows info about the mod
    &lt;debug&gt; - boolean
      Shows additional info,
      needs permission level 3 and above.
      Defaults to false

/claimr claiminfo
  &gt; /claiminfo

/claimr claim
  &gt; /claim

/claimr unclaim
  &gt; /unclaim

/claimr unclaimall
  &gt; /unclaimall

/claimr group
  &gt; /group

/claiminfo             
  - Shows info about the chunk at your posisition.
    Example response:
      Chunk Location: minecraft:overworld;0:1
      This chunk is unclaimed
   
    Example response:
      Chunk Location: minecraft:overworld;6:-9
      Claimed by: SwanX1

/claim &lt;group&gt;
  - Claims the current chunk,
    throws error if you aren't a manager of the group
    or if the chunk is already claimed.
    &lt;group&gt; - string;
      Determines the group you are claiming this chunk for.

/unclaim
- Unclaims the current chunk, 
  throws error if you aren't a manager of the group
  or if the chunk is not claimed by the group.

/unclaimall &lt;group&gt;
- Unclaims all chunks claimed by the group,
  throws error if you aren't a manager of the group.
  &lt;group&gt; - string;
  Determines the group you are unclaiming this chunk from.

/group create &lt;group&gt;
- Creates a group,
  throws error if group already exists.

/group add &lt;group&gt; &lt;...players&gt;
- Adds members to group, will not overwrite rank,
  throws error if group doesn't exist
  or if your rank is too low (needs to be >1)

/group remove &lt;group&gt; &lt;...players&gt;
- Removes members from group, will not remove managers, unless you're the owner,
  throws error if group doesn't exist
  or if your rank is too low (needs to be >1)

/group promote &lt;group&gt; &lt;...players&gt;
- Promotes members in group,
  throws error if group doesn't exist
  or if your rank is too low (needs to be >2)

/group demote &lt;group&gt; &lt;...players&gt;
- Demotes members in group,
  throws error if group doesn't exist
  or if your rank is too low (needs to be >2)

/group transferownership &lt;group&gt; &lt;player&gt;
- Transfers ownership of group to new player, you stay as group manager,
  throws error if group doesn't exist
  or if your rank is too low (needs to be >2)
</pre>