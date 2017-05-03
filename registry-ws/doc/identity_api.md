### Endpoints

#### Public endpoints

| URL | Method | Success | Description |
| --- | --- | --- | --- |
| `/user/login` | `GET` | `{USER}`, `{SESSION}` | Logs in a user with HTTP Basic Auth returning the session token as a cookie in the response |
| `/user/logout?allSessions` | `GET` | `204` | Logs out the session or optionally all sessions for the authenticated account (cookie based auth) |
| `/user` | `PUT` | `204` | Updates the user, verifying the authenticated user is authorised (himself). Verifies [fields](#update-user-fields) and returns `422` if the user can not be updated. |
| `/user` | `GET` | `{USER}`, `{SESSION}` | Gets a user, using the session token (cookie) for Auth |

#### Trusted applications only
| URL | Method | Success | Description |
| --- | --- | --- | --- |
| `/user` | `POST` | `201` | Creates a user. Verifies [required fields](#create-user-fields) and returns `422` if the user can not be created. The appkey must be provided as `x-gbif-user` |
| `/user/confirm` | `POST` | `201` `{USER}`, `{SESSION}` | Confirms that the user have access to that mail. The target user must be provided as `x-gbif-user` and the `challengeCode` in the post body (as JSON). User is logged in immediately if `challengeCode` is valid. |
| `/user/resetPassword` | `POST` | `204` | Send user a mail with link to reset password. The target user (userName or email) must be provided as `x-gbif-user`. |
| `/user/challengeCodeValid?challengeCode={challengeCode}` | `GET` | `204` | Utility for the web app to determine if the token is the currently valid challenge for the user. Returns `204` if so (app will then present the new password form) or `401` if the token is not considered authorized to change the password. The target user must be provided as `x-gbif-user`. |
| `/user/updatePassword` | `POST` | `201` `{USER}`, `{SESSION}` | Updates the password for the user by accepting the `challengeCode` and `password` in the post body (as JSON). Returns `401` if the token is not authorized to change the password. Deletes all user tokens and return a new login token to set as cookie. The target user must be provided as `x-gbif-user`. |

#### Administrative console only
| URL | Method | Success | Description |
| ------------- |---------| ---------|---|
| `/user/{userID}` | `GET` | `{USER}` | Gets the user role is authorised to view the user (e.g. enable admins to edit account details) |
| `/user/{userID}` | `PUT` | `204` | Allowing admin to edit user accounts |


### Create user fields

| Property      | Mandatory | Comments  |
| ------------- |:---------:| ---------:|
| userName      | x         | between 3 and 64 "^[a-z0-9_.-]+$" |
| email         | x         |  |
| password      | x         |  |
| firstName     |           |  |
| lastName      |           |  |
| settings      |           | key/value map |

### Update user fields

| Property      | Comments  |
| ------------- |---------:|
| firstName     | |
| lastName      | |
| email         | |
| settings      | key/value map |

