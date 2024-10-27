#### 用户登录注册
1. 用户登录
  - 密码登录 http://localhost:8080/users/login
  - 第三方登录(Gitee) http://localhost:8080/oauth/login
    - 自动跳转到gitee的登录页面，登录成功后，会通过`callback`返回，并且携带code参数
    - 通过`code`参数，尝试使用其中的`uuid`进行第三方登录
      - 如果`uuid`存在，则直接登录，否则，跳转到注册页面（todo)
      - 返回token

2. 用户注册
  - http://localhost:8080/users/register
    - 账号密码注册
    - todo:第三方登录但是没有注册 

#### AI聊天
1. 获取某个用户的聊天记录列表
  - http://localhost:8080/getConversationIDList
    - 参数：`userID`
    - todo:使用token等信息

2. 获取某个用户的某个聊天记录 `Get`
  - http://localhost:8080/chat
    - 参数：`userID` `conversationID`
    - todo:使用token等信息

3. 聊天
  - http://localhost:8080/chat
    - 参数：ChatRequest {`userID` `conversationID` `content`}
    - 首先判断是否有该用户的这条聊天记录，如果没有，则创建一条新的聊天记录，返回值中会表示这是否是新的聊天
    - 返回值：`assistedContent` 表示机器人的回复，`isNew`表示是否为新的聊天记录
    - todo: 返回值不知道怎么写