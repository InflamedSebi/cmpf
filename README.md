# CMPF - Curse modpack finder

Since Curse doesn't offer a way to find modpacks that include modA and modB, I made a tool for it.

This might be important:

* I'll **_not_** maintain this project  as it was a one time thing to create it. However if you want to contribute and develop it further see 'Contribute' section below.

## How does it work?

Basically you run the .jar file (made for Java 8 but might work with 7 too).  
On the upper left side these is the "Start"-button and below an input area for the mods to search packs for.  
you can paste the url from a mod there or type its name like

```
https://minecraft.curseforge.com/projects/actually-additions
actually-additions
```

After all your prefered mods are present hit the start button.  
It will now internally open the curse webpage for each mod and download the 'dependents' list.  
This might take a while since actually additions has 152 pages of dependents alone and all the other mods will need to be processed too.  
Luckyly the progress is written to the output panel below, so you can see it.  
Once a mod finishes downloading it will create a 'cache' folder in the tools working directory, so you don't have to download this stuff again unless its outdated.  
If you feel like a redownload is needed, just delete the 'cache' folder.  
Once all downloads are done, it will create a 'results.ini' containing all modpacks that have your mod selection in common.  
I would suggest to open it with a text editor that highlights ini syntax and can open links, like notepad++, but the choice is yours.
But there is more!
You can now hit the start button again (that turned into a 'Fetch Meta' button meanwhile) to get a little more information about the packs.  
For example it is not possible to tell which version of Minecraft the pack was made for and when it last updated, or if it even has files for download.  
This is exactly what will be added to the 'results.ini' by fetching meta.

Before fetching meta:

```
[vocalcraft]
link=https://minecraft.curseforge.com/projects/vocalcraft
[all-the-mods-lite]
link=https://minecraft.curseforge.com/projects/all-the-mods-lite
[inner-hope]
link=https://minecraft.curseforge.com/projects/inner-hope
[in-the-beginning]
link=https://minecraft.curseforge.com/projects/in-the-beginning
[reboottech]
link=https://minecraft.curseforge.com/projects/reboottech
[zapharia-apocalypse]
link=https://minecraft.curseforge.com/projects/zapharia-apocalypse
[tobis-project]
link=https://minecraft.curseforge.com/projects/tobis-project
```

After fetching meta:

```
[1.12]
the-rise-of-technomancy-modpack=06/06/2018-https://minecraft.curseforge.com/projects/the-rise-of-technomancy-modpack
technological-codex=04/06/2018-https://minecraft.curseforge.com/projects/technological-codex
[1.7]
technologyadvanced=08/04/2018-https://minecraft.curseforge.com/projects/technologyadvanced
the-mad-mage-pack=01/06/2018-https://minecraft.curseforge.com/projects/the-mad-mage-pack
xyniik-gaming=18/08/2017-https://minecraft.curseforge.com/projects/xyniik-gaming
nordliv-modpack=19/01/2017-https://minecraft.curseforge.com/projects/nordliv-modpack
tabby-and-storm=01/04/2018-https://minecraft.curseforge.com/projects/tabby-and-storm
tickle-me-elmo=18/04/2017-https://minecraft.curseforge.com/projects/tickle-me-elmo
[1.10]
the-super-over-the-top-wonder-minemania=07/09/2016-https://minecraft.curseforge.com/projects/the-super-over-the-top-wonder-minemania
[1.11]
swords-of-evolution=25/03/2018-https://minecraft.curseforge.com/projects/swords-of-evolution
cake-factory=18/08/2017-https://minecraft.curseforge.com/projects/cake-factory
```

Looks a lot better right? And meta will also be saved in the cache files.  
The last feature is the right panel. If you reopen the tool it will display a list of all cached mods (blue) and mods which were added sometime, but don't have a cache file (red).  

## Building it your own

Basically you only need this repo, jgoodies-forms-1.8.0.jar, jgoodies-common-1.8.1.jar and xswingx.jar.  
Oh and Java 8 SDK of course.  
Now build it like any other Java app. I don't know Gradle and stuff, so no build script for you, sorry. :(   

## Contributing

Since this is not in active development and will probably never be, as long as it is working the way it is now, there is a way to make it better:

* I'll accept PRs containing code cleanup / documentation / general improvements to readability / stability.
* I'll most likely accept PRs containing bugfixes / refactored inconsistencies / general code improvements.
* I'll probably not accept PRs containing complex GUI builds (except it makes me get rid of jgoodies/xswingx) or add even more 3rd party libraries.

Feel free to leave issues or take one and fulfill it.
You can of course add your name to the authors if you made improvements (not for just renaming stuff though) 

## Authors

* **Sebastian 'Inflamedsebi' Pammler** - *Initial work*

## License

This project is licensed under the AGPL License - see the [agpl-3.0.txt](agpl-3.0.txt) file for details
