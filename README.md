# UUIDProvider
A bukkit plugin that provides players UUID support for other plugins.
This plugin has been tested on Cauldron 1.6.4 and 1.7.10.

All my plugin's builds can be downloaded from http://kaikk.net/mc/#bukkit

##Configuration
You can use this plugin without any configuration, but I recommend a MySQL database in order to enable cache.
Edit plugins/UUIDProvider/config.yml and set your database account.
If you're running multiple servers, be sure to use the same MySQL database to improve performances!

##Developers: how to use it

Add this plugin in your project's build path.

Most important methods:
- (UUID) UUIDProvider.get(OfflinePlayer)
- (OfflinePlayer) UUIDProvider.get(UUID)

Please report any issue! Suggestions are well accepted!

##Support my life!
I'm currently unemployed and I'm studying at University (Computer Science).
I'll be unable to continue my studies soon because I need money.
If you like this plugin and you run it fine on your server, please <a href='http://kaikk.net/mc/#donate'>consider a donation</a>!
Thank you very much!