- Allow the battery bar page to open (the code existed I just forgot to hook it into the menus)
- Make QS (and QQS) time/date/battery taps open to clock/calendar/battery-usage
- Add option to hide the back and language selector in keyboard navbar
- Remove arbitrary systemui restart every time the system boots up (it'll now restart only when it needs to)
- Transition from libxposed api 101 to 102 (making future updates easier and adding all xposed features) (read more: https://t.me/s/PixelXpert_Github/1155)
- Remove KSU (and KSU-Next) hooks since they were dead code
- New translations! ( contribute on https://crowdin.com/project/pixelxpert-next )
[As a reminder: Google has still not given the new SystemUI changes to the P11, so I still can't fix some issues introduced in the September/October build]
