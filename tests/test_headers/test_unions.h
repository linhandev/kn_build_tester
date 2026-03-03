#ifndef TEST_UNIONS_H
#define TEST_UNIONS_H

/* Named union */
union NamedUnion {
    int ival;
    float fval;
    char cval;
};

/* Anonymous union via typedef */
typedef union {
    int integer;
    double real;
} NumberUnion;

#endif
