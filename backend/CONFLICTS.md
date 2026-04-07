This document outlines the mandatory workflow for handling code conflicts within this project.

A conflict is identified when a Pull Request to the develop or main branches cannot be merged automatically by GitHub or when a local merge fails.
At first the devOps team will inform the developers who authored the conflicting lines of code regarding the conflict.
If the respective team doesn't resolve the conflict within an agreed upon timeframe, the DevOps team will intervene to resolve it.

Any conflicts will be resolved with the following steps:
1. Locate the issue and update the local environment.
2. Open the conflicted files and locate the conflict markers (<<<<<<< and >>>>>>>).
3. Resolve the conflict by combining the code or selecting the correct version based on the earlier team consultation.
4. Remove all Git conflict markers and save the files.

Once the conflict is resolved locally, the following must be completed before pushing:

1. Compilation Check: The application must compile successfully without errors.
2. Unit Tests: Run the Unit Test Integration suite (as defined in the DevOps checklist) to ensure the Game Logic remains intact.
3. Commit & Push.

The resolved PR must be reviewed by at least one member of the DevOps team and one member of the affected feature team to verify that no functional logic was lost during the merge.
