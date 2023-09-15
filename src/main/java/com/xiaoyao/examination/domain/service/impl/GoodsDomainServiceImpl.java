package com.xiaoyao.examination.domain.service.impl;

import com.xiaoyao.examination.domain.entity.Goods;
import com.xiaoyao.examination.domain.enums.GoodsStatus;
import com.xiaoyao.examination.domain.enums.GoodsType;
import com.xiaoyao.examination.domain.repository.GoodsRepository;
import com.xiaoyao.examination.domain.service.GoodsDomainService;
import com.xiaoyao.examination.exception.ErrorCode;
import com.xiaoyao.examination.exception.ExaminationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GoodsDomainServiceImpl implements GoodsDomainService {
    private final GoodsRepository goodsRepository;

    @Override
    public void createGoods(Goods goods) {
        // 判断套餐类别是否存在
        boolean isTypeExist = false;
        for (GoodsType goodsType : GoodsType.values()) {
            if (goodsType.getType() == goods.getType()) {
                isTypeExist = true;
                break;
            }
        }
        if (!isTypeExist) {
            throw new ExaminationException(ErrorCode.GOODS_TYPE_NOT_EXIST);
        }
        // 判断套餐名称是否已存在
        if (goodsRepository.countGoods(goods.getName()) == 1) {
            throw new ExaminationException(ErrorCode.GOODS_NAME_EXIST);
        }

        goods.setSalesVolume(0);
        goods.setStatus(GoodsStatus.ON.getStatus());
        goods.setUpdateTime(LocalDateTime.now());
        goods.setCreateTime(LocalDateTime.now());
        goodsRepository.insert(goods);
    }

    @Override
    public Map<Integer, String> getAllGoodsType() {
        Map<Integer, String> types = new HashMap<>();
        for (GoodsType value : GoodsType.values()) {
            types.put(value.getType(), value.getName());
        }
        return types;
    }

    @Override
    public void changeStatus(long id, int status) {
        // 判断状态是否存在
        boolean isStatusExist = false;
        for (GoodsStatus goodsStatus : GoodsStatus.values()) {
            if (goodsStatus.getStatus() == status) {
                isStatusExist = true;
                break;
            }
        }
        if (!isStatusExist) {
            throw new ExaminationException(ErrorCode.GOODS_STATUS_NOT_EXIST);
        }

        Goods goods = new Goods();
        goods.setId(id);
        goods.setStatus(status);
        goodsRepository.update(goods);
    }

    @Override
    public List<Goods> searchGoods(long page, long size, String code, String name, Integer type, Integer status, long[] total) {
        return goodsRepository.searchGoods(page, size, code, name, type, status, total);
    }
}
