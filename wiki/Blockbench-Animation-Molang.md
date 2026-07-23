# Blockbench 动画与 Molang

RIAutomobility 可以直接播放 `.bbmodel` 中的骨骼动画，并允许在关键帧的 X、Y、Z 数值中填写 Molang 表达式。借助项目提供的车辆查询量，同一个动画可以实时响应转向、车轮旋转、引擎、涡轮、Boost 和乘客视角。

> [!IMPORTANT]
> RIAutomobility 实现的是面向 BB 动画的 **Molang 子集**，不是 Minecraft Bedrock 的完整 Molang 运行时。请只使用本文列出的语法、函数和查询量；未列出的 Bedrock 查询、动画控制器、脚本语句等均不受支持。

当前支持的 Blockbench 工程格式版本为 `4.10` 至 `5.0`。版本更旧的工程需要使用较新的 Blockbench 重新保存，版本高于 `5.0` 的工程暂不接受。

## 快速上手

1. 在 Blockbench 中打开车辆部件的 `.bbmodel` 工程并切换到“动画”模式。
2. 创建骨骼动画，并为骨骼添加位置、旋转或缩放关键帧。
3. 在关键帧的 X、Y、Z 输入框中直接填写表达式，例如：

   ```molang
   query.vehicle_steering * 30
   ```

4. 保存 `.bbmodel`，再通过 RIAutomobility 的车辆导入台导入并导出车辆包。

下面是几个常用表达式。

方向盘随车辆转向：

```molang
query.vehicle_steering * 30
```

车轮骨骼使用车辆当前的累计车轮角度：

```molang
query.vehicle_wheel_angle
```

引擎运行时产生轻微振动：

```molang
query.vehicle_engine_running ? math.sin(query.life_time * 720) * 0.4 : 0
```

让某个骨骼有限度地跟随 0 号座位乘客的水平视角：

```molang
math.clamp(query.vehicle_passenger_view_yaw(0), -45, 45)
```

## 可用查询量

查询量可以使用完整写法 `query.xxx`，也可以缩写为 `q.xxx`。名称不区分大小写。

| 查询量 | 返回值 | 说明 |
| --- | ---: | --- |
| `query.anim_time` | 秒 | 当前动画时间。循环动画会在动画长度内循环，单次动画到达末尾后保持在末帧。 |
| `query.life_time` | 秒 | 车辆自创建以来的连续时间，不随动画循环归零。渲染时包含局部 Tick 插值。 |
| `query.is_on_ground` | `0` 或 `1` | 车辆在地面上时为 `1`，否则为 `0`。 |
| `query.vehicle_steering` | 数值 | 当前转向值。通常可乘以所需的最大转向角度，例如 `q.vehicle_steering * 30`。 |
| `query.vehicle_wheel_angle` | 度 | 当前累计车轮旋转角度，可直接用于轮子或传动部件的旋转通道。 |
| `query.vehicle_engine_running` | `0` 或 `1` | 引擎正在运行时为 `1`。 |
| `query.vehicle_turbo_charge` | 数值 | 当前涡轮蓄力的原始值。该值不是 `0` 到 `1` 的归一化比例。 |
| `query.vehicle_boost_timer` | Tick | 当前剩余的 Boost 时间；未处于 Boost 时为 `0`。 |

当模型没有真实车辆上下文，或查询量当前不可用时，查询结果为 `0`。例如导入界面的预览车辆没有真实乘客，因此乘客视角查询会返回 `0`；预览中的转向值也固定为 `0`。

### 乘客视角查询

| 函数 | 返回值 | 说明 |
| --- | ---: | --- |
| `query.vehicle_passenger_view_yaw(seat)` | 度 | 指定座位乘客相对车辆朝向的水平视角，结果会环绕到 `[-180, 180)` 度。 |
| `query.vehicle_passenger_view_pitch(seat)` | 度 | 指定座位乘客的俯仰视角。 |

`seat` 是从 `0` 开始的**可视座位索引**，与车辆导入台中的座位顺序一致。参数可以是表达式，但最终结果必须是非负整数。座位不存在、无人乘坐、参数为负数或小数时返回 `0`。

```molang
q.vehicle_passenger_view_yaw(0)
```

参数表达式会先求值，因此下面读取 2 号座位：

```molang
q.vehicle_passenger_view_pitch(1 + 1)
```

## 数值与布尔值

Molang 表达式最终都返回数值：

- `true` 等于 `1`，`false` 等于 `0`。
- 比较和逻辑运算的结果也是 `1` 或 `0`。
- `0` 和 `NaN` 视为假，其他数值视为真。
- 支持普通小数和科学计数法，例如 `0.25`、`1e-3`。
- 支持常量 `math.pi` 和 `math.e`。

## 运算符

下表从高到低列出运算符优先级：

| 优先级 | 运算符 | 说明 |
| ---: | --- | --- |
| 1 | `()` | 括号 |
| 2 | 一元 `+`、`-`、`!` | 正号、负号、逻辑非 |
| 3 | `^` | 幂，右结合 |
| 4 | `*`、`/`、`%` | 乘、除、取余 |
| 5 | `+`、`-` | 加、减 |
| 6 | `<`、`<=`、`>`、`>=` | 比较 |
| 7 | `==`、`!=` | 相等、不等 |
| 8 | `&&` | 逻辑与，支持短路求值 |
| 9 | `||` | 逻辑或，支持短路求值 |
| 10 | `条件 ? 真值 : 假值` | 三元条件，右结合 |

建议在幂运算与负数混用时显式加括号，例如 `-(2 ^ 2)` 或 `(-2) ^ 2`，避免对优先级产生误解。

```molang
q.vehicle_engine_running && q.is_on_ground ? 1 : 0
```

## 数学函数

三角函数使用**度**而不是弧度；反三角函数也返回度。

| 函数 | 说明 |
| --- | --- |
| `math.abs(x)` | 绝对值 |
| `math.sin(x)`、`math.cos(x)`、`math.tan(x)` | 三角函数，参数单位为度 |
| `math.asin(x)`、`math.acos(x)`、`math.atan(x)` | 反三角函数，结果单位为度 |
| `math.atan2(y, x)` | 二参数反正切，结果单位为度 |
| `math.sqrt(x)` | 平方根 |
| `math.floor(x)` | 向负无穷取整 |
| `math.ceil(x)` | 向正无穷取整 |
| `math.round(x)` | 按 `floor(x + 0.5)` 取整；例如 `1.5` 得到 `2`，`-1.5` 得到 `-1` |
| `math.trunc(x)` | 去掉小数部分，向零取整 |
| `math.exp(x)` | `e` 的 `x` 次幂 |
| `math.ln(x)` | 自然对数 |
| `math.sign(x)` | 返回 `-1`、`0` 或 `1` |
| `math.pow(x, y)` | `x` 的 `y` 次幂，等价于 `x ^ y` |
| `math.mod(x, y)` | 取余，等价于 `x % y` |
| `math.clamp(x, min, max)` | 将 `x` 限制在 `[min, max]` 范围内 |
| `math.lerp(a, b, t)` | 线性插值，结果为 `a + (b - a) * t` |
| `math.min(a, ...)` | 返回一个或多个参数中的最小值 |
| `math.max(a, ...)` | 返回一个或多个参数中的最大值 |
| `math.random()` | 生成 `[0, 1)` 范围的随机数 |
| `math.random(min)` | 使用 `min` 和 `1` 作为随机范围端点 |
| `math.random(min, max)` | 结果为 `min + 随机数 * (max - min)` |

`math.random` 会在动画重新采样时重新生成值，因此适合逐帧抖动，不适合生成每辆车固定不变的随机参数。

## 自定义变量

Blockbench 的动画变量占位符可以把长表达式拆成可复用变量。在 Blockbench 的动画变量占位符设置中，每行填写一个赋值：

```molang
v.steer_angle = q.vehicle_steering * 30;
v.engine_shake = q.vehicle_engine_running ? math.sin(q.life_time * 720) * 0.4 : 0;
v.driver_yaw = math.clamp(q.vehicle_passenger_view_yaw(0), -45, 45);
```

随后可以在关键帧输入框中使用：

```molang
v.steer_angle
```

变量规则：

- 每个变量必须单独占一行；行末分号可写可不写。
- `v.name` 与 `variable.name` 等价。
- `q.name` 与 `query.name`、`t.name` 与 `temp.name`、`c.name` 与 `context.name` 分别等价。
- 变量名不区分大小写。
- 变量是按需计算的表达式，不是可以逐帧修改或保存状态的存储槽。
- 变量可以引用其他变量，最大依赖深度为 64 层。
- 循环引用会导致模型格式错误，例如 `v.a = v.b` 与 `v.b = v.a`。
- 当前实现优先读取 `.bbmodel` 顶层的 `animation_variable_placeholders`；仅当该字段为空时才读取旧字段 `variable_placeholders`。

没有定义且不属于本文查询表的变量会返回 `0`。这可以避免缺失变量直接中断渲染，但也意味着拼写错误可能表现为动画不动。

## 动画播放规则

### 支持的内容

- 动画目标：Blockbench 骨骼（`bone`）或类型为空的动画器。
- 动画通道：`position`、`rotation`、`scale`。
- 插值模式：`linear`、`step`、`catmullrom`、`bezier`。
- 关键帧 X、Y、Z：数字或本文支持的单个 Molang 表达式。
- 动画时间和关键帧时间的单位均为秒。
- 表达式在客户端渲染时求值，并使用局部 Tick 插值，所以车辆查询的变化可以平滑反映到模型上。

RIAutomobility 会保留 Blockbench 关键帧的进入值和离开值。两个关键帧之间的插值模式由前一个关键帧决定。贝塞尔插值使用 Blockbench 保存的相对时间、相对数值控制柄。

无法识别的插值标签会按线性插值处理，而不是报错。

### 循环与结束

- 动画 `loop` 为 `loop` 或 `true` 时，`query.anim_time` 按动画长度循环。
- 其他 `loop` 值按单次播放处理，到达动画长度后保持末帧。
- 动画长度小于或等于 `0` 时，动画时间不会循环或截断。

### 选择哪条动画

车辆导入台生成的组件不指定动画名，因此会播放 `.bbmodel` 中的**第一条动画**。如果手工维护车辆组件 JSON，可以在 `model` 对象中使用 `bb_animation` 指定动画名称或 UUID：

```json
{
  "model": {
    "type": "bbmodel",
    "model_id": "example:riautomobility/frame/demo",
    "texture": "example:textures/entity/automobile/frame/demo.png",
    "bbmodel": "example:models/entity/automobile/frame/demo.bbmodel",
    "bb_animation": "drive"
  }
}
```

名称或 UUID 不存在时不会回退到第一条动画，该模型会按静态模型渲染。

## 完整示例

假设模型中有以下骨骼：

- `steering_wheel`：方向盘；
- `engine`：引擎；
- `driver_headrest`：驾驶员头枕；
- `boost_gauge`：Boost 仪表指针。

先定义动画变量：

```molang
v.steering = q.vehicle_steering * 30;
v.engine_offset = q.vehicle_engine_running ? math.sin(q.life_time * 720) * 0.25 : 0;
v.driver_look = math.clamp(q.vehicle_passenger_view_yaw(0), -35, 35);
v.boost_angle = q.vehicle_boost_timer > 0 ? 90 : math.clamp(q.vehicle_turbo_charge / 35, 0, 1) * 75;
```

然后在相应骨骼的旋转或位置通道中引用这些变量：

| 骨骼 | 通道 | 轴 | 表达式 |
| --- | --- | --- | --- |
| `steering_wheel` | 旋转 | 按模型实际朝向选择 | `v.steering` |
| `engine` | 位置 | Y | `v.engine_offset` |
| `driver_headrest` | 旋转 | Y | `v.driver_look` |
| `boost_gauge` | 旋转 | 按指针轴选择 | `v.boost_angle` |

骨骼局部轴取决于模型的层级、枢轴和朝向。如果动画方向相反，直接在表达式前加负号，例如 `-v.steering`。

## 当前不支持

以下功能即使属于 Bedrock Molang 或 Blockbench 动画系统，也不在当前实现范围内：

- 除本文列出的查询量之外的 `query.*`；
- Bedrock 动画控制器、状态切换、动画混合与过渡；
- `pre_animation`、控制器变量、实体属性、材质或纹理表达式；
- 赋值语句、`return`、循环、代码块、数组、结构体和字符串；
- 粒子、声音、时间轴脚本和动画事件；
- `position`、`rotation`、`scale` 之外的动画通道；
- 同时叠加或混合多条 Blockbench 动画。

不要从 Bedrock 文档直接复制未在本页列出的函数。未知函数会触发“Unsupported Molang function”模型格式错误；未知普通变量则返回 `0`。

## 排错

### 动画完全不播放

1. 确认动画器目标是骨骼，通道是位置、旋转或缩放。
2. 确认需要播放的动画位于动画列表第一项，或组件 JSON 的 `bb_animation` 与名称/UUID 完全一致。
3. 确认骨骼仍在 Outliner 中，动画目标 UUID 与骨骼 UUID 一致。
4. 查看客户端日志中是否有 `Invalid Blockbench Molang expression` 或 `Unsupported Molang function`。

### 表达式结果始终为 0

1. 检查查询量或变量拼写；未知变量会静默返回 `0`。
2. 如果使用乘客视角查询，确认座位索引存在且该座位有人乘坐。
3. 如果只在导入预览中测试，注意预览没有真实乘客，且转向值固定为 `0`。
4. 自定义变量必须每行一个赋值，不能在同一行用多个分号连续定义。

### 模型报告循环变量依赖

检查动画变量占位符中的引用链。直接循环和间接循环都会报错：

```molang
v.a = v.b;
v.b = v.a;
```

### 动画方向相反

这是模型局部轴和车辆坐标方向共同作用的结果。优先在 Blockbench 中确认骨骼枢轴与局部轴，再对表达式取负值，例如 `-q.vehicle_wheel_angle`。
