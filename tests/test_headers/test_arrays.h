#ifndef TEST_ARRAYS_H
#define TEST_ARRAYS_H

/* fixed-size array field in struct */
struct WithFixedArray {
    int data[10];
};

/* multidimensional array field */
struct WithMultidimArray {
    int matrix[3][4];
};

/* incomplete array (flexible array member) */
struct WithFlexArray {
    int count;
    int items[];
};

/* fixed array parameter */
void take_fixed_array(int arr[10]);

/* incomplete array parameter */
void take_incomplete_array(int arr[]);

#endif
