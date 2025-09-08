# Reflection

**R1. What threads are there in your solution, and how do they communicate?**

**R2. Why do we always put wait in a while loop? Why wouldn’t if work?**

**R3. Mathematician Augustus De Morgan (1806–1871) is known for having formulated two laws, which can be expressed in Java as**
!(a && b) == (!a || !b) !(a || b) == (!a && !b)
How can these laws be useful when implementing monitor methods? (Think about your while loops.)

**R4. Why can’t we call the LiftView method moveLift() in a monitor?**

**R5. Suppose a monitor includes a single attribute x. Also suppose the monitor includes the following method, waiting for x to change:**
````java
public class Example {
private int x;
// ...
public synchronized int awaitX() throws InterruptedException {
while (x == 0) {
wait();
}
return x;
}
// ... other methods ...
````
The monitor also has other methods. Some methods (like awaitX above) read x, and some modify x.
How can you decide which method(s) should call notify/notifyAll?

**R6. Why can wait only be called in a synchronized method (or a method called by another synchronized method for the same object)?**