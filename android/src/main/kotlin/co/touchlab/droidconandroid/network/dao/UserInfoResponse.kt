package co.touchlab.droidconandroid.network.dao

/**
 * Created by kgalligan on 7/26/14.
 */
data class UserInfoResponse(var user: UserAccount, var speaking: Array<EventInfo>, var attending: Array<EventInfo>)

data class LoginResult(var uuid: String, var userId: Long, var user: UserAccount, var speaking: Array<EventInfo>, var attending: Array<EventInfo>)

data class UserAccount(var id: Long, var name: String, var profile: String,
                       var avatarKey: String, var userCode: String, var company: String,
                       var twitter: String, var linkedIn: String, var website: String, var following: Boolean)

data class EventInfo(var id: Long, var name: String, var description: String)
