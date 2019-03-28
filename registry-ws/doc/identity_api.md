### Endpoints

#### Public endpoints

| URL | Method | Success | Description |
| --- | --- | --- | --- |
| `/user/login` | `GET` | `{USER}` | Logs in a user with HTTP Basic Auth returning the user in the response |
| `/user/changePassword`| `PUT` | `204` | Allows user to change its own password using HTTP Basic Auth |

#### Administrative and Trusted application endpoints
| URL | Method | Success | Description |
| --- | --- | --- | --- |
| `admin/user` | `POST` | `201` | Creates a user. Verifies [required fields](#create-user-fields) and returns `422` if the user can not be created. The appkey must be provided as `x-gbif-user` |
| `admin/user/{username}` | `GET` | `{USER}` | Gets the user role is authorised to view the user (e.g. enable admins to edit account details) |
| `admin/user/{username}` | `PUT` | `204` | Updates the user. Verifies [fields](#update-user-fields) and returns `422` if the user can not be updated. |
| `admin/user/confirm` | `POST` | `201` `{USER}` | Confirms that the user have access to that mail. The target user must be provided as `x-gbif-user` and the `confirmationKey` in the post body (as JSON). |
| `admin/user/find` | `GET` | `{USER}` | Get user by systemSettings key/value. E.g. `?auth.github.id=12345` |
| `admin/user/resetPassword` | `POST` | `204` | Send user a mail with link to reset password. The target user (userName or email) must be provided as `x-gbif-user`. |
| `admin/user/confirmationKeyValid?confirmationKey={confirmationKey}` | `GET` | `204` | Utility for the web app to determine if the confirmationKey is the currently valid one for the user. Returns `204` if so (app will then present the new password form) or `401` if the confirmationKey is not considered authorized to change the password. The target user must be provided as `x-gbif-user`. |
| `admin/user/updatePassword` | `POST` | `201` `{USER}`| Updates the password for the user by accepting the `confirmationKey` and `password` in the post body (as JSON). Returns `401` if the token is not authorized to change the password. Deletes all user tokens and return a new login token to set as cookie. The target user must be provided as `x-gbif-user`. |

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
All the following fields are required in order to apply the update.

| Property      | Comments  |
| ------------- |----------:|
| username      | for reference only, can not be updated |
| firstName     | |
| lastName      | |
| email         | |
| settings      | key/value map |
| systemSettings      | key/value map |
