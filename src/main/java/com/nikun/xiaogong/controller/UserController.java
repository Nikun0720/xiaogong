package com.nikun.xiaogong.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nikun.xiaogong.common.R;
import com.nikun.xiaogong.entity.User;
import com.nikun.xiaogong.service.UserService;
import com.nikun.xiaogong.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    @PostMapping("/sendMsg")
    public R<String> sendMsg(@RequestBody User user, HttpSession session) {

        // 获取手机号
        String phone = user.getPhone();

        // 如果传过来的手机号为空，则发送失败，返回错误信息
        if(StringUtils.isEmpty(phone)) {
            R.error("短信发送失败");
        }

        // 生成随机的4位验证码
        String code = ValidateCodeUtils.generateValidateCode(4).toString();
        log.info("code:{}", code);

        // 调用阿里云提供的短信服务API完成发送短信
        //SMSUtils.sendMessage("小工外卖", "", phone, code);

        // 需要将生成的验证码保存到Session
        // session.setAttribute(phone, code);

        // 将生成的验证码缓存到redis中，并且设置有效期为5分钟
        stringRedisTemplate.opsForValue().set(phone, code, 5, TimeUnit.MINUTES);

        return R.success("手机验证码短信发送成功");

    }

    /**
     * 移动端用户登录
     * @param map
     * @return
     */
    @PostMapping("/login")
    public R<User> login(@RequestBody Map map, HttpSession session) {
        // log.info("map:{}", map);

        // 获取手机号
        String phone = map.get("phone").toString();

        // 获取验证码
        String code = map.get("code").toString();

        // 从Session中获取保存的验证码
        // Object codeInSession = session.getAttribute(phone);

        // 从redis中获取缓存的验证码
        Object codeInSession = stringRedisTemplate.opsForValue().get(phone);

        // 进行验证码的比对（页面提交的验证码和Session中保存的验证码进行比对）
        if(codeInSession == null || !codeInSession.equals(code)) {
            // 比对失败，返回错误信息
            R.error("登录失败");

        }

        // 如果比对成功，则登录成功
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, phone);
        User user = userService.getOne(queryWrapper);

        if(user == null) {
            // 判断当前手机号对应的用户是否为新用户，如果是新用户就自动完成注册
            user = new User();
            user.setPhone(phone);
            //user.setStatus(1);      // 可以不设置，数据库默认值为1
            userService.save(user);
        }

        session.setAttribute("user", user.getId());
        log.info("id:{}", user.getId());

        // 如果用户登录成功，删除redis中缓存的验证码
        stringRedisTemplate.delete(phone);

        return R.success(user);
    }

    /**
     * 移动用户退出
     * @param session
     * @return
     */
    @PostMapping("/loginout")
    public R<String> loginout(HttpSession session) {

        session.removeAttribute("user");

        return R.success("退出成功");
    }

}
