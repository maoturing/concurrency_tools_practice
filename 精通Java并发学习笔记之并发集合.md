# 7. 并发容器

## 7.1 并发容器概览

ConcurrentHashMap：线程安全的HashMap

CopyOnWriteArrayList：线程安全的List

BlockingQueue：阻塞队列接口，适合用于数据共享的通道

ConcurrentLinkedQueue：高效的非阻塞并发队列，可以看做线程安全的LinkedList

## 7.2 并发容器的历史

Vector 与 Hashtable 都是 JDK1.0时代的产物，使用 synchronized 来保证集合操作线程安全，也是并发初学者能想到的实现线程安全集合的方式。

Vector 是线程安全的 ArrayList，**使用 synchronized 来保证线程安全**，但是因为添加和取出操作都是用同一把锁，导致效率低下，现在 Vector 这个集合已经被淘汰。

```java
// Vector使用sync来保证添加、删除、获取元素操作的线程安全，效率很低
    public synchronized boolean add(E e) {
        modCount++;
        ensureCapacityHelper(elementCount + 1);
        elementData[elementCount++] = e;
        return true;
    }

    public synchronized E remove(int index) {
        // ......
        elementData[--elementCount] = null; // Let gc do its work

        return oldValue;
    }

    public synchronized E get(int index) {
        if (index >= elementCount)
            throw new ArrayIndexOutOfBoundsException(index);

        return elementData(index);
    }
```

与之类似的还有 Hashtable，是线程安全的HashMap，也是**使用 synchronized 来保证线程安全**，效率低下已被淘汰。（需要注意Hashtable中T小写）



## 7.3 ConcurrentHashMap

> 为什么 HashMap 是线程不安全的？

1. 同时put 碰撞导致数据丢失，两个线程put时，槽中没有数据，同时将数据插入某一个槽，后者会覆盖前者
2. 同时put 扩容导致数据丢失
3. 死循环造成 CPU 100%

### 7.3.1 CPU 100%问题

在JDK7中，HashMap 在扩容时容易出现死循环造成 100% 问题，具体分析参考下面文章

https://coolshell.cn/articles/9606.html
https://www.jianshu.com/p/1e9cf0ac07f4
https://www.jianshu.com/p/619a8efcf589
https://blog.csdn.net/loveliness_peri/article/details/81092360
https://cloud.tencent.com/developer/article/1120823
https://www.cnblogs.com/developer_chan/p/10450908.html
https://www.yuque.com/dangxianyuyudaoniu/kclb/2019


map
why
hashmap分析
1.7与1.8
也不是线程安全
案例





集合历史过时并发容器
ConcurrentHashMap
CopyOnWriteArrayList
并发队列
总结




[HashMap在并发场景下踩过的坑](https://sq.163yun.com/blog/article/195978631766196224)