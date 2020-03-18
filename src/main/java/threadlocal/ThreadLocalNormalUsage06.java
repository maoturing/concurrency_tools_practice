package threadlocal;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 模仿Spring中使用ThreadLocal，每个请求都是一个线程，每个线程都有User对象的副本
 * 而且不用层层传递user对象，直接保存到UserContextHolder静态变量中
 */
public class ThreadLocalNormalUsage06 {
    public static void main(String[] args) {
        new UserService().process();
        new OrderService().process();
    }
}

class UserService {
    public void process() {
        User user = new User("mau");
        UserContextHolder.holder.set(user);
        System.out.println("UserService："+user.name);
    }
}

class OrderService {
    public void process() {
        User user = UserContextHolder.holder.get();
        System.out.println("OrderService："+user.name);
    }
}

class UserContextHolder {
    public static ThreadLocal<User> holder = new ThreadLocal<>();
}

class User {
    String name;
    public User(String name) {
        this.name = name;
    }
}