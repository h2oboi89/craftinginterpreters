//>= Chunks of Bytecode 1
#ifndef clox_memory_h
#define clox_memory_h

//>= Strings 1
#include "object.h"

//>= Chunks of Bytecode 1
#define ALLOCATE(type, count) (type*)reallocate(NULL, 0, sizeof(type) * (count))
#define FREE(type, pointer) reallocate(pointer, sizeof(type), 0)

#define GROW_CAPACITY(capacity) ((capacity) < 8 ? 8 : (capacity) * 2)

#define GROW_ARRAY(previous, type, oldCount, count) \
    (type*)reallocate(previous, sizeof(type) * (oldCount), sizeof(type) * (count))
#define FREE_ARRAY(type, pointer, oldCount) \
    reallocate(pointer, sizeof(type) * (oldCount), 0)

void* reallocate(void* previous, size_t oldSize, size_t newSize);
//>= Garbage Collection 1

void grayObject(Obj* object);
void grayValue(Value value);
void collectGarbage();
//>= Strings 1
void freeObjects();
//>= Chunks of Bytecode 1

#endif
