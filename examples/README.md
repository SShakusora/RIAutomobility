# Single-Component RIAuto Examples

English | [中文](#中文)

## English

Every directory under `components/` is the unpacked source of one `.riauto` file and declares exactly one component:

- `frame-jsonem`: `examplepack:example_buggy` frame
- `wheel-jsonem`: `examplepack:example_buggy_wheel` wheel
- `frame-gecko`: `examplepack:example_buggy_gecko` frame
- `wheel-gecko`: `examplepack:example_buggy_gecko_wheel` wheel
- `frame-bbmodel`: `examplepack:example_buggy_bbmodel` frame
- `wheel-bbmodel`: `examplepack:example_buggy_bbmodel_wheel` wheel

To install an example, ZIP the contents of one directory without an extra enclosing directory, rename it to `<name>.riauto`, and copy it into the game directory's `riautomobility/` folder. Install as many of the six files as needed.

Each archive contains:

- one root `riauto.json` whose `frames`, `wheels`, and `engines` arrays contain exactly one id in total;
- one matching component definition under `data/<namespace>/riautomobility/...`;
- only the assets needed by that component.

RIAutomobility rejects archives that declare multiple components or contain undeclared component definition files. JsonEM, GeckoLib, and BBModel assets remain supported. Dedicated servers distribute each required component archive independently to clients.

## 中文

`components/` 下的每个目录都是一个 `.riauto` 文件的未打包源码，并且只声明一个组件：

- `frame-jsonem`：`examplepack:example_buggy` 车架
- `wheel-jsonem`：`examplepack:example_buggy_wheel` 车轮
- `frame-gecko`：`examplepack:example_buggy_gecko` 车架
- `wheel-gecko`：`examplepack:example_buggy_gecko_wheel` 车轮
- `frame-bbmodel`：`examplepack:example_buggy_bbmodel` 车架
- `wheel-bbmodel`：`examplepack:example_buggy_bbmodel_wheel` 车轮

安装示例时，将其中一个目录内的内容直接压缩，不要额外套一层目录；把文件改名为 `<名称>.riauto`，再放入游戏目录的 `riautomobility/` 文件夹。可以按需同时安装这六个文件中的任意多个。

每个归档只包含：

- 一个根目录 `riauto.json`，其中 `frames`、`wheels`、`engines` 三个数组合计恰好包含一个 id；
- `data/<命名空间>/riautomobility/...` 下与该 id 对应的一份组件定义；
- 该组件实际需要的资源。

RIAutomobility 会拒绝声明多个组件或携带未声明组件定义文件的归档。JsonEM、GeckoLib 和 BBModel 资源仍受支持。专用服务器会按组件分别向客户端分发所需的 RIAuto 文件。
