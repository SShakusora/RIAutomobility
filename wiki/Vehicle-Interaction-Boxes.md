# OBB 车辆交互盒

车辆交互盒是车架定义中的非实体 OBB（有向包围盒）。它们可以响应左键、右键以及
对应的 Shift 组合，但不参与方块碰撞、实体推挤、车辆碰撞、碾压或伤害计算，
也不会作为实体生成到世界中。

[返回 Wiki 首页](Home.md) · [Blockbench 动画与 Molang](Blockbench-Animation-Molang.md) ·
[车辆钥匙与多座位](Vehicle-Keys-and-Multi-Seat.md)

## 配置格式

在车架组件根对象中添加 `interaction_boxes`：

```json
{
  "disable_hitbox_interactions": true,
  "interaction_boxes": [
    {
      "id": "trunk",
      "center": {"x": 0, "y": 0.75, "z": 1.2},
      "size": {"x": 1.2, "y": 0.6, "z": 0.25},
      "rotation": {"x": 0, "y": 0, "z": 0},
      "actions": [
        {"type": "open_container"},
        {
          "type": "molang",
          "channel": 1,
          "operation": "toggle",
          "value": 1,
          "transition_ticks": 8
        }
      ]
    }
  ]
}
```

- `id`：车架内唯一的小写 ID，长度为 1 到 64，可包含数字、下划线、点和连字符。
- `center`：相对车架模型原点的盒中心，单位为方块；整个对象省略时为 `(0, 0, 0)`。
  更换不同半径的轮胎不会改变交互盒与车架之间的相对位置。
- `size`：沿交互盒自身局部轴计算的完整 X/Y/Z 尺寸；整个对象省略时为 `(1, 1, 1)`，
  每一轴必须大于 0 且不超过 32。
- `rotation`：交互盒相对车辆局部坐标的 X/Y/Z 欧拉角，单位为度；各轴省略时为 `0`。
- `actions`：命中后依次执行的一个或多个动作。

根对象的 `disable_hitbox_interactions` 默认为 `false`。设为 `true` 后，车辆主实体碰撞箱
和所有附加 `HitboxEntity` 都不再处理右键，玩家只能通过交互盒执行上车、打开容器或
Molang 等载具功能。该选项不影响物理碰撞、伤害检测和 F3+B 显示。

每个车架最多定义 64 个交互盒，每个盒必须包含 1 到 16 个动作。`center` 各轴的
绝对值不能超过 32，`size` 各轴不能超过 32；所有位置、尺寸和旋转值都必须是有限数。

## 在车辆导入台中编辑

1. 切换到“外壳”页的“交互”分页。
2. 使用交互盒标题右侧的 `+`/`-` 新增或删除交互盒；点击标题可在已有交互盒之间切换。
3. 设置唯一 ID、中心、完整尺寸和三轴旋转。当前交互盒会在预览中显示轮廓。
4. 使用动作标题右侧的 `+`/`-` 管理动作；点击动作标题可循环选择当前动作。
5. 点击动作类型按钮，在“打开容器”“上车”和“Molang”之间切换。

新建交互盒的默认尺寸为 `1 × 1 × 1`，并带有一条 0 号通道的 `pulse` Molang 动作。
导出前编辑器会检查 ID、尺寸、动作和重复 ID；无效草稿不会生成 `.riauto`。

## 动作

### 打开容器

```json
{"type": "open_container"}
```

打开车辆自身的 54 格库存。物品不存放在交互盒中。容器动作始终要求玩家具有车辆访问权限。
即使 JSON 中显式写入 `"requires_access": false`，加载时也会强制恢复为需要权限。
该动作固定由普通右键触发，不能配置 `trigger`。

### 上车

```json
{"type": "mount", "seat": 0}
```

`seat` 是从 0 开始的座位索引，有效范围为 `0` 到 `255`。省略 `seat` 或设置为
`-1` 时使用第一个可用乘客座位（从 1 号位开始查找，不会自动占用驾驶位）。
该动作固定由普通右键触发，不能配置 `trigger`。

`mount` 默认要求车辆访问权限，但可以显式写入 `"requires_access": false`，让没有
钥匙的玩家进入指定乘客位。0 号驾驶位无论该字段如何设置，仍会再次执行车辆钥匙检查。
目标座位已有玩家时动作失败；若座位上是非玩家实体，则会先让该实体离座。

### Molang 状态

```json
{
  "type": "molang",
  "trigger": "shift_right_click",
  "channel": 0,
  "operation": "pulse",
  "value": 1,
  "duration_ticks": 10,
  "transition_ticks": 4,
  "requires_access": false
}
```

- `channel`：`0` 到 `31`。
- `trigger`：`left_click`、`right_click`、`shift_left_click` 或
  `shift_right_click`；省略时默认为 `right_click`。
- `operation`：`set`、`toggle` 或 `pulse`；省略时默认为 `pulse`。
- `value`：目标值，加载时限制为 `0` 到 `1`；省略时默认为 `1`。
- `duration_ticks`：`pulse` 保持目标状态的时长；省略时默认为 `10`。
- `transition_ticks`：进入目标状态和脉冲返回时的插值时长；省略时默认为 `0`。
- `requires_access`：是否要求车辆访问权限。容器和上车动作默认为 `true`，
  Molang 动作默认为 `false`。

Blockbench 表达式使用 `q.vehicle_interaction(channel)` 读取插值后的状态，
使用 `q.vehicle_interaction_time(channel)` 读取最近一次变化后的秒数。
同一个交互盒可以添加多条使用不同 `trigger` 和 `channel` 的 Molang 动作，从而让
左键、右键、Shift+左键和 Shift+右键分别控制不同动画状态。

### 三种 Molang 操作

每个 `channel` 都是一条取值范围为 `0` 到 `1` 的独立状态通道，初始值为 `0`。
Molang 动作修改通道状态，Blockbench 动画再通过
`q.vehicle_interaction(channel)` 使用该状态。三种 `operation` 的区别如下：

| 操作 | 用途 | 触发后的目标值 | 状态是否自动复原 | `duration_ticks` |
| --- | --- | --- | --- | --- |
| `set` | 明确打开、关闭或设置某个连续状态 | 始终设置为 `value` | 否 | 不生效 |
| `toggle` | 用同一个输入在开与关之间切换 | 在 `0` 和 `value` 之间切换 | 否 | 不生效 |
| `pulse` | 按钮、喇叭、短促灯光等临时状态 | 先设置为 `value`，计时结束后回到 `0` | 是 | 控制从触发到开始复原的时间 |

#### `set`：直接设置

`set` 每次触发都会把通道的目标值设置为 `value`，并保持该值，直到另一个动作再次
修改相同通道。它适合用不同输入分别控制打开和关闭，或将动画设置到某个固定进度。

例如，Shift + 右键打开部件：

```json
{
  "type": "molang",
  "trigger": "shift_right_click",
  "channel": 0,
  "operation": "set",
  "value": 1,
  "transition_ticks": 5
}
```

再为同一个交互盒添加一条 Shift + 左键动作，将相同通道关闭：

```json
{
  "type": "molang",
  "trigger": "shift_left_click",
  "channel": 0,
  "operation": "set",
  "value": 0,
  "transition_ticks": 5
}
```

在这个例子中，通道会用 5 tick 从当前值平滑变化到 `1` 或 `0`。
`duration_ticks` 对 `set` 没有影响，可以省略。

#### `toggle`：开关切换

`toggle` 适合车门、车灯或可展开部件等双稳态功能。第一次触发时，通道从关闭状态
切换到 `value`；再次触发相同动作时，通道切换回 `0`。

```json
{
  "type": "molang",
  "trigger": "right_click",
  "channel": 1,
  "operation": "toggle",
  "value": 1,
  "transition_ticks": 6
}
```

切换判断使用通道上一次的**目标值**：目标值大于等于 `0.5` 时视为开启，
下一次触发会关闭到 `0`；否则下一次触发会切换到 `value`。因此 `toggle` 通常应将
`value` 设置为 `1`，或至少设置为 `0.5`。如果 `value` 小于 `0.5`，它不会被视为
开启状态，后续触发仍会将目标设置为该 `value`，无法正常切换回 `0`。

`transition_ticks` 同时作用于打开和关闭过程。即使在插值尚未结束时再次触发，
也会从当时的实际插值值平滑转向新的目标。`duration_ticks` 对 `toggle` 没有影响。

#### `pulse`：定时脉冲

`pulse` 适合只需短暂激活的动画，例如按下按钮、鸣笛、闪灯或机械部件短促动作。
触发时，通道向 `value` 变化；到达 `duration_ticks` 指定的结束时刻后，通道自动
向 `0` 返回。

```json
{
  "type": "molang",
  "trigger": "left_click",
  "channel": 2,
  "operation": "pulse",
  "value": 1,
  "duration_ticks": 10,
  "transition_ticks": 2
}
```

脉冲计时从玩家触发动作的瞬间开始，包含进入目标值所花费的插值时间。以上配置的
时间线为：

1. 第 0 tick 触发，从当前值开始向 `1` 插值。
2. 第 2 tick 到达 `1`。
3. 第 10 tick 开始从当前值向 `0` 返回。
4. 第 12 tick 完成返回。

如果 `duration_ticks` 小于 `transition_ticks`，通道会在尚未到达 `value` 时开始
返回，因此动画可能永远达不到配置的最大值。再次触发相同脉冲会以当前插值值为起点，
重新设置目标并重新开始脉冲计时。

### 参数对操作的影响

| 参数 | `set` | `toggle` | `pulse` |
| --- | --- | --- | --- |
| `channel` | 要写入的持久状态通道 | 要切换的持久状态通道 | 要临时激活的状态通道 |
| `trigger` | 触发本次设置的输入 | 触发开关切换的输入 | 触发或重新触发脉冲的输入 |
| `value` | 新的固定目标值 | 开启状态的目标值 | 脉冲激活阶段的目标值 |
| `duration_ticks` | 忽略 | 忽略 | 从触发到开始返回 `0` 的 tick 数 |
| `transition_ticks` | 从当前值到 `value` 的插值时间 | 打开和关闭时的插值时间 | 进入 `value` 和返回 `0` 各自的插值时间 |
| `requires_access` | 是否在执行前检查车辆访问权限 | 是否在执行前检查车辆访问权限 | 是否在执行前检查车辆访问权限 |

`value` 会被限制在 `0` 到 `1`；`duration_ticks` 和 `transition_ticks` 的有效范围均为
`0` 到 `72000`。Minecraft 通常每秒运行 20 tick，因此 20 tick 约等于 1 秒。
`transition_ticks` 为 `0` 时状态会立即跳到目标值；`pulse` 的返回过程同样会立即完成。

对同一通道执行任何新动作都会替换该通道当前的目标状态，并从当时的实际插值值继续，
所以可以平滑地中断尚未完成的动画。为了避免同一次输入中的多条动作互相覆盖，不建议
在同一个交互盒中为相同 `trigger` 和相同 `channel` 配置多条 Molang 动作。

`q.vehicle_interaction_time(channel)` 返回该通道自最近一次状态变化开始后经过的秒数。
执行新动作时计时会归零；`pulse` 开始自动返回 `0` 时也会再次归零。

### 多动作与权限检查

一次输入只会执行当前交互盒中 `trigger` 匹配的动作，并保持它们在 `actions` 数组中的顺序。
`open_container` 和 `mount` 固定匹配 `right_click`；Molang 动作使用各自的 `trigger`。

权限检查按本次匹配到的整组动作进行：只要其中一条动作需要访问权限，未授权玩家的
整组动作都会被拒绝，后续无需权限的 Molang 动作也不会单独执行。因此若希望无钥匙玩家
也能播放某个动画，应把它放到单独的交互盒，或为它选择不会与受限动作重合的触发方式。

### 编辑器动画预览

在车辆导入界面的“交互”分页中选中 Molang 动作时，预览载具会从状态 `0` 自动执行
一次当前动作。修改 `channel`、`operation`、`value`、`duration_ticks` 或
`transition_ticks` 后也会使用新参数重新播放。动作参数下方的“重新播放 Molang”
按钮可以随时从初始状态再次播放，便于反复检查 `pulse` 等短动画。

预览使用与游戏内相同的 `q.vehicle_interaction(channel)` 和
`q.vehicle_interaction_time(channel)` 查询入口以及 `set`、`toggle`、`pulse`
状态语义。只有当前 BBModel 的动画表达式实际引用了所选 `channel` 时，模型才会出现
对应变化；没有定义该频道动画时，预览保持不变。切换到其他动作、交互盒或编辑分页时，
预览频道状态会被清除，避免先前动作残留并干扰当前动画。

需要测试模型时，可以导入
[animated_frame_demo.bbmodel](../art/examples/animated_frame_demo.bbmodel)。
该示例分别使用 `0` 到 `4` 号频道演示车门、引擎盖、灯组、尾翼和脉冲按钮动画。

## 交互规则

客户端只负责找出视线中最近的 OBB，并发送车辆 ID、交互盒 ID、使用手和本次触发方式。
服务端会重新计算玩家视线、交互距离、最近交互盒和方块遮挡，然后从服务端车架定义
筛选并执行与触发方式匹配的动作。客户端不能指定具体动作或伪造命中位置。

如果视线中存在更近的方块或无关实体，则保留原版交互。车辆自身及其物理
`HitboxEntity` 不会遮挡该车辆的交互盒：即使没有启用 `disable_hitbox_interactions`，
同一次输入同时命中交互盒和同车碰撞箱时也始终优先执行交互盒。

左键只有在视线命中的交互盒存在对应的 `left_click` 或 `shift_left_click` Molang 动作时
才会被拦截；否则攻击输入保持原版行为。副手不会让同一次右键重复执行动作。

## 调试显示

在客户端按 `F3+B` 开启原版碰撞箱显示时，交互 OBB 会以绿色线框显示。
线框跟随车辆的位置、偏航、俯仰和侧倾，并使用与服务端交互射线检测相同的旋转变换。
再次按 `F3+B` 即可关闭。
