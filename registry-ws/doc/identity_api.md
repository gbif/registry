### Endpoints

| URL | Method | Success | Public | Auth | Description |
| --- | --- | --- | --- | --- | --- |
| `/user` | `GET` | `{USER}` | Y | Y | Gets a user, using the session token (cookie) for Auth |
| `/user/login` | `GET` | `{USER}` | Y | Y | Logs in a user with HTTP Basic Auth returning the session token as a cookie in the response |
| `/logout?allSessions` | `GET` | `204` | Y | Y | Logs out the session or optionally all sessions for the authenticated account (cookie based auth) |
| `/user/{userID}` | `GET` | `{USER}` | Y | Y | Gets the user, verifying the session token (cookie) is the user, or the user role is authorised to view the user (e.g. enable admins to edit account details) |
| `/user` | `POST` | `204` | N | N | Creates a user. Internal (by trusted application). Verifies required fields as agreed [here](https://gist.github.com/cgendreau/c4c42f4a5f3fb82385d020784cf0d3a5) |
| `/user/confirm?challengeCode={UUID}` | `PUT` | `200` | N | N | Confirms that the user have access to that mail. app key and x-gbif-user. mail to express contains user and challenge. endpoint returns login token so that the user is logged in immediately |
| `/user` | `PUT` | `204` | Y | Y | Updates the user, verifying the authenticated user is authorised (session tied to the `userID` or is an admin) |
| `/user/reset-password?mailOrUsername` | `PUT` | `200` | Y | N | send user a mail with link to reset password |
| `/user/{userID}/tokenValid?token={token}` | `GET` | `204` | N | N | Utility for the web app to determine if the token is the currently valid challenge for the user. Returns `204` if so (app will then present the new password form) or `401` if the token is not considered authorized to change the password |
| `/user/{userID}/updatePassword` | `POST` | `204` | N | N | Updates the password for the user by accepting the `token={token}` and `password={newPassword}` in the form. Returns `204` if accepted or `401` if the token is not authorized to change the password delete all user tokens and return a new login token to set as cookie|

### Create user

| Verb      | URL | Authentication  |
| --------- |:---------:| ---------:|
| POST      | /user    | appKey |


| Property      | Mandatory | Comments  |
| ------------- |:---------:| ---------:|
| userName      | x         | between 3 and 64 "^[a-z0-9_.-]+$" |
| email         | x         |  |
| firstName     | x         |  |
| lastName      | x         |  |
| settings      |           | key/value map |


Expected Response: 201 Created

### Confirm challenge code
| Verb      | URL | Authentication  |
| --------- |:---------:| ---------:|
| PUT      | /user/confirm?challengeCode={UUID} | appKey including x-gbif-user |

Expected Response: 200 OK

### Update user
| Verb      | URL | Authentication  |
| --------- |:---------:| ---------:|
| PUT      | /user | session token |

| Property      | Comments  |
| ------------- |---------:|
| firstName     | |
| lastName      | |
| settings      | key/value map |

Expected Response: 200 OK
