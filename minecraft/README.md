# Bouncer
A Minecraft Forge plugin to keep non Destiny.gg subscribers away from a Minecraft server

# Development
The preferred IDE is IntelliJ IDEA with the Kotlin plugin installed, as this project was written in Kotlin.

### Setup:
- `./gradlew cleanCache cleanIdea setupDevWorkspace idea`

### Compilation:
- `./gradlew build`

The mod will then be located in `build/libs/`

### To run a dev server:
- `./gradlew runServer`

# Installation
- Place the jar file in your `mods` directory
- Create a `bouncer_config.json` file that is placed within the directory that has the `mods` directory. Here's the structure of the file:
```json
{
  "secret": "<your api secret>",
  "trusted_uuids": [
    "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
  ]
}
```

# Usage
Bouncer installs 1 server command: `togglebouncer (on|off)`, which is used to enable / disable the authorization portion of the plugin.

# License
```
   Copyright 2017 Destiny.gg

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```
