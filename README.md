# DifficultyVoter
A Bukkit plugin that can change server difficulty by your players votes without operator in-game or in console.

## Why?
Imagine: you're now playing on Minecraft sever with friends who dont really like hardcore gameplay. But, suddenly, you suggest friends to challenge yourself, for ex.: trial chamber, raid etc. But there is no operator on server or near the console - thats why this plugin exist. Any player can create a vote to change server difficulty. You can also specify a permissions for some ranks on your server.

## Usage
/dv <easy | normal | hard> - starts vote for everyone, will end in 30 seconds;
/dv vote <yes | no> - command to vote, only works when there's active voting;
/dv forcestop - stops voting, only available for administrator

## Permissions:
dv.*
dv.bypasscooldown
dv.forcestop
