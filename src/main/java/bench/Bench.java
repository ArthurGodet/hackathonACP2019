/*
@author Arthur Godet <arth.godet@gmail.com>
@since 07/03/2019
*/
package bench;

import data.Factory;
import data.input.Instance;
import model.RNMP;

import java.io.IOException;

public class Bench {

    public static void main(String[] args) throws IOException {
        Instance instance = Factory.fromFile("data/EASY_5_3.json", Instance.class);
        RNMP rnmp = new RNMP(instance);
        rnmp.solve("5m");
    }
}
