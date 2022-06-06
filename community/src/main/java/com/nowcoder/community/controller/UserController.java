package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.FollowService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

@Controller
@RequestMapping("/user") //设置访问路径
public class UserController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);//声明Looger变量

    @Value("${community.path.upload}")
    private String uploadPath; //声明上传头像地址

    @Value("${community.path.domain}")
    private String domain; //声明域名地址

    @Value("${server.servlet.context-path}")
    private String contextPath; //声明项目的访问路径

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.header.name}")
    private String headerBucketName;

    @Value("${qiniu.bucket.header.url}")
    private String headerBucketUrl;

    @LoginRequired
    @RequestMapping(path = "/setting", method = RequestMethod.GET)
    public String getSettingPage(Model model){ //客服端上传， 异步提交表单 直接将表单传给七牛云服务器
        //上传文件的名称
        String fileName = CommunityUtil.generateUUID(); //随机生成文件名
        //设置响应信息
        StringMap policy = new StringMap();
        policy.put("returnBody", CommunityUtil.getJSONString(0)); //上传成功后返回一个 code:0 的json字符串， 代表成功
        //生成上传的凭证
        Auth auth = Auth.create(accessKey, secretKey); //实例化一个auth对象
        String uploadToken = auth.uploadToken(headerBucketName, fileName, 3600, policy); //生成一个上传凭证（一个字符串）， (上传空间名，文件名，指定过期时间（1个小时）， 响应信息)

        //将uploadToken和fileName传入model使用
        model.addAttribute("uploadToken", uploadToken);
        model.addAttribute("fileName", fileName);

        return "/site/setting";
    }

    //更新头像的路径
    @RequestMapping(path = "/header/url", method = RequestMethod.POST)
    @ResponseBody
    public String updateHeaderUrl(String fileName){
        if(StringUtils.isBlank(fileName)){
            return CommunityUtil.getJSONString(1, "文件名不能为空！");
        }

        String url = headerBucketUrl + "/" + fileName; //（用户头像）文件的访问路径
        userService.updateHeader(hostHolder.getUser().getId(), url);//更新该用户的头像 (用户Id, 用户头像的访问路径)

        return CommunityUtil.getJSONString(0); //返回更新成功的消息
    }

    //废弃原头像上传方法
    @LoginRequired
    @RequestMapping(path = "/upload", method = RequestMethod.POST) //声明方法的访问路径
    public String uploadHeader(MultipartFile headerImage, Model model){
        if(headerImage == null){ //若上传的图片文件为空
            model.addAttribute("error", "您还没有选择图片"); //设置错误提示
            return "/site/setting";//然后重新返回上传图片的页面
        }

        String fileName = headerImage.getOriginalFilename();//获取用户上传头像的原始图片名
        String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);//（截取图像格式的后缀名（jpg，png））从原始图片名中截取从最后一个"."往后的字符串
        if(StringUtils.isBlank(suffix)){ //如果后缀名为空
            model.addAttribute("error", "文件的格式不正确!");
            return "/site/setting";//然后重新返回上传图片的页面
        }

        //生成随机文件名
        fileName = CommunityUtil.generateUUID() + "." + suffix;

        //确定文件存放的路径
        File dest = new File(uploadPath + "/" + fileName);
        try {
            //存储文件
            headerImage.transferTo(dest); //将headerImage的内容写入到目标文件dest中
        } catch (IOException e) {
            logger.error("上传文件失败:" + e.getMessage());
            throw new RuntimeException("上传文件失败，服务器发生异常", e); //抛出异常
        }

        //更新当前用户的头像的路径（web访问路径），而不是本地路径d盘 c盘之类的。
        //例如http://localhost:8080/community/user/header/xxx.png
        User user = hostHolder.getUser();
        String headerUrl = domain + contextPath + "/user/header/" + fileName; //设置允许外界访问的web路径
        userService.updateHeader(user.getId(), headerUrl);//调用Service层的方法，更新用户头像

        return "redirect:/index"; //重定向到首页的访问路径
    }

    //废弃原头像上传方法
    @RequestMapping(path = "/header/{fileName}", method = RequestMethod.GET)
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response){
        //服务器存放的路径
        fileName = uploadPath + "/" + fileName;

        //文件的后缀
        String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);//（截取图像格式的后缀名（jpg，png））从原始图片名中截取从最后一个"."往后的字符串
        //响应图片
        response.setContentType("image/" +suffix);
        try ( //java7规定 ， try打个()，在()内写入的内容会自动在最后添加finally块，对该部分的进行关闭（前提是有class方法）
                OutputStream os = response.getOutputStream(); //获取响应的字节流  //springmvc会自动关闭OutputStream输出流，因为response由springmvc管理
                FileInputStream fis = new FileInputStream(fileName); //创建文件的输入流 ，读取fileName文件得到一个输入流 ，输入流自己创建的不会自动关闭，需要手动关闭
        ){
            byte[] buffer = new byte[1024];//得到输入流以后，开始输出
            int b = 0;
            while((b = fis.read(buffer)) != -1){ //不等于-1代表读到了数据
                os.write(buffer,0, b); //输出数据
            }

        } catch (IOException e) {
            logger.error("读取头像失败: " + e.getMessage());
        }

    }
    // 修改密码
    @RequestMapping(path = "/updatePassword", method = RequestMethod.POST)
    public String updatePassword(String oldPassword, String newPassword, Model model) {
        User user = hostHolder.getUser();
        Map<String, Object> map = userService.updatePassword(user.getId(), oldPassword, newPassword);
        if (map == null || map.isEmpty()) {
            return "redirect:/logout";
        } else {
            model.addAttribute("oldPasswordMsg", map.get("oldPasswordMsg"));//供给模板调用
            model.addAttribute("newPasswordMsg", map.get("newPasswordMsg"));
            return "/site/setting";
        }
    }

    //个人主页
    @RequestMapping(path = "/profile/{userId}", method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId, Model model){
        User user = userService.findUserById(userId);
        if(user == null){
            throw new RuntimeException("该用户不存在");
        }

        //用户
        model.addAttribute("user", user);
        //点赞数量
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount", likeCount);

        //关注数量
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount); //关注数量传入模板
        //粉丝数量
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId );
        model.addAttribute("followerCount", followerCount); //粉丝数量
        //是否已关注
        boolean hasFollowed = false;
        if(hostHolder.getUser() != null){
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowed", hasFollowed);

        return "/site/profile";
    }
}
