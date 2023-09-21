package com.xiaoyao.examination.domain.repository;

import com.xiaoyao.examination.domain.entity.Goods;

import java.util.List;
import java.util.Map;

public interface GoodsRepository {
    void insert(Goods goods);

    void update(Goods goods);

    List<Goods> searchGoods(long page, long size,
                            String code, String name, Integer type, Integer status, Integer sort,
                            long[] total);

    long countGoodsByNameOrCode(String name, String code);

    Goods queryGoodsById(long id);

    Goods getUpdateGoodsById(long id);

    void deleteGoods(List<Long> ids);

    long countDontDeletedGoods(List<Long> ids);

    int getGoodsStatusById(long id);

    List<Goods> getRecommendGoods(int sort, int count);

    List<Goods> searchGoodsByPage(int pass, int size, String name, Integer type, String gender,
                                  String bottomPrice, String topPrice, String order);

    Goods getSnapshotGoodsById(long goodsId);

    Map<Long, Long> countGoodsByDiscountIds(List<Long> discountIds);
}
