# TargetApp
Part of the Geofencing package. This app is used by the person being tracked. App is now usable.

## Todo

Alert user when location is off.

Add geofences to the user map?

Handle connection resets when trying to communicate with the server. As in, if no response is received in time, log an error or something.

## Done

Update intervals are now set every time TargetApp sends a location update, based on the fastest interval set by the overseers.

Implemented background running of the TargetApp.

Implement one-time use tracking tokens to be used with server, for security, in preparation for the implementation of OverseerApp.

App can now be used to manage the user and update their location in the server database.
