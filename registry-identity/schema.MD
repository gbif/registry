### The table schema design

The account, roles, settings, and active session are all managed in a single table for simplicity.  It allows database operations to be atomic updates.

