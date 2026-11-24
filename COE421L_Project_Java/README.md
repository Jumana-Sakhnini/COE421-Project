# COE421L_Project_Java - Sentry Turret Security Control

## Run in Eclipse
1. File > Import > General > Existing Projects into Workspace > select the COE421L_Project_Java folder > Finish.
2. Right-click `Sentry Turret - Simulation.launch` > Run As > it opens the security screen with no hardware.
3. For the real board: edit `Sentry Turret - XBee.launch` (VM argument `-Dgame.serial.port=COM8` -> your XBee's COM port), then Run As.
   (Or Run Configurations: Program arguments `--connect`, VM arguments `-Dgame.serial.port=COMx`.)

## Game
- The window asks for the number of security officers and their names.
- Each officer presses START SHIFT on the screen to begin the shift. Only losing all 3 hearts ends it.
- RED = shoot the middle target (+15), BLUE = press the button under that LED (+10),
  GREEN = repeat the access code with the joystick (+20; LED1 LEFT, LED2 up/down, LED3 RIGHT).
  3rd correct in a row and later: +5 combo.
- A heart is lost for: a wrong action (the same challenge stays), a time-out, or reacting to a fake flicker.
  A shot with no red target and joystick moves with no code are ignored (noise filter).
- Level 1: slow, gun + buttons, 5 correct to pass. Level 2: a notch faster, + joystick, 5 correct.
  Level 3: endless, faster with every correct action, fake dim flickers.
- CLAP: slows the game down (+0.7 s, max +2.1 s).
- After the last officer: session leaderboard + all-time top 5 (leaderboard.txt).

## Simulator keys
SPACE fire | 1 2 3 buttons | LEFT, UP/DOWN, RIGHT joystick | C clap
