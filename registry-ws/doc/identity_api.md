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