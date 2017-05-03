### Endpoints

#### Public endpoints

| URL | Method | Success | Description |
| --- | --- | --- | --- |
| `/user/login` | `GET` | `{USER}`, `{SESSION}` | Logs in a user with HTTP Basic Auth returning the session token as a cookie in the response |
| `/user/logout?allSessions` | `GET` | `204` | Logs out the session or optionally all sessions for the authenticated account (cookie based auth) |
| `/user` | `PUT` | `204` | Updates the user, verifying the authenticated user is authorised (himself) |
| `/user` | `GET` | `{USER}`, `{SESSION}` | Gets a user, using the session token (cookie) for Auth |

#### Trusted applications only
| URL | Method | Success | Description |
| --- | --- | --- | --- |
| `/user` | `POST` | `201` | Creates a user. Internal (by trusted application). Verifies required fields as agreed [here](#create-user-fields) |
| `/user/confirm?challengeCode={UUID}` | `POST` | `201` `{USER}`, `{SESSION}` | Confirms that the user have access to that mail. app key and x-gbif-user. mail to express contains user and challenge. endpoint returns login token so that the user is logged in immediately |
| `/user/resetPassword?identifier={userName or email}` | `POST` | `201` | Send user a mail with link to reset password |
| `/user/challengeCodeValid?challengeCode={challengeCode}` | `GET` | `204` | Utility for the web app to determine if the token is the currently valid challenge for the user. Returns `204` if so (app will then present the new password form) or `401` if the token is not considered authorized to change the password |
| `/user/updatePassword` | `POST` | `200` `{USER}`, `{SESSION}` | Updates the password for the user by accepting the `challengeCode={challengeCode}` and `password={newPassword}` in the form. Returns `204` if accepted or `401` if the token is not authorized to change the password. Delete all user tokens and return a new login token to set as cookie|

#### Administrative console only
| URL | Method | Success | Description |
| ------------- |---------| ---------|---|
| `/user/{userID}` | `GET` | `{USER}` | Gets the user role is authorised to view the user (e.g. enable admins to edit account details) |


### Create user fields

| Property      | Mandatory | Comments  |
| ------------- |:---------:| ---------:|
| userName      | x         | between 3 and 64 "^[a-z0-9_.-]+$" |
| email         | x         |  |
| password      | x         |  |
| firstName     | x         |  |
| lastName      | x         |  |
| settings      |           | key/value map |

### Update user fields

| Property      | Comments  |
| ------------- |---------:|
| firstName     | |
| lastName      | |
| settings      | key/value map |

