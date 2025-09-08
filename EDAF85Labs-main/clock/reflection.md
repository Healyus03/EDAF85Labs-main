# Reflection 

### **R1. Why is mutual exclusion needed in your program?**
Ett koncept i programmering när vi vill köra flera trådar men undvika att de körs samtidigt i kritiska sektioner av koden. Detta är viktigt för att undvika problem som när trådar fösöker komma åt samma data samtidigt.
I vårt fall använder vi det eftersom vi har olika trådar som står för olika delar av alarmklockan. 

### **R2. How can you use a Semaphore (or Lock) for mutual exclusion?**
Våra getter och setter metoder i ClockMonitor, SSOT, använder vi en lås för att säkerställa att endast en tråd åt gången kan komma åt och ändra klockans tid. 


### **R3. How can you use a Semaphore for signaling between threads?**
I vårat fall använder vi en semafor genom att in.getSemaphore().acquire(). Detta gör att tråden inväntar en release från en annan tråd, i vårt fall en input från emulatorn. 

### R4. **How do you use the Monitor design pattern in your design?**
Den håller clockstate i en enda instans så att inte olika delar av programmet har olika versioner av tiden, SSOT. Den ser också till att alla trådar som behöver komma åt tiden gör det på ett säkert sätt genom att använda lås för att undvika att flera trådar ändrar tiden samtidigt.

### R5. **What does it mean to say that a thread is blocked?**
Att den inte kan fortsätta köra förrän den får tillgång till en resurs den behöver, som en lås eller semafor. I vårt fall kan en tråd vara blockerad när den försöker komma åt klockans tid om en annan tråd redan håller låset för att ändra tiden. Den måste vänta tills låset är frigjort innan den kan fortsätta.

### **R6. In your implementation work, tasks I5–I6, you encountered inconsistent output: a clock time value that didn’t correspond to the time set. How could this inconsistency arise? How can it be prevented?**
För att två trådar ändrar tiden samtidigt utan att använda lås, vilket kan leda till att en tråd skriver över den andra trådens ändringar. Detta kan förhindras genom att använda lås för att säkerställa att endast en tråd åt gången kan ändra klockans tid.

### **R7. The test in step I5 runs for 30 seconds. Why does it have to run for so long? Can we guarantee that this time is sufficient to find the race conditions we are looking for?**
Det behöver köra så länge för att öka chansen att olika trådar försöker komma åt och ändra klockans tid samtidigt. 