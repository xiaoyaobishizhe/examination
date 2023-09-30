package com.xiaoyao.examination.common.interfaces.goods.response;

import java.io.Serializable;
import java.util.List;

public class AllGoodsTypeResponse implements Serializable {
    private List<Type> types;

    public List<Type> getTypes() {
        return types;
    }

    public void setTypes(List<Type> types) {
        this.types = types;
    }

    public static class Type implements Serializable {
        private int type;
        private String name;

        public Type(int type, String name) {
            this.type = type;
            this.name = name;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
