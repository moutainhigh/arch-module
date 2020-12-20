package com.unichain.pay.channel.mfe88.directive;

import com.alibaba.fastjson.JSONObject;
import com.unichain.pay.channel.mfe88.Mfe88ChannelDirecvite;
import com.unichain.pay.channel.mfe88.Mfe88PayRequestHandler;
import com.unichain.pay.channel.mfe88.domain.BankcardPayParam;
import com.unichain.pay.channel.mfe88.domain.BankcardPrePayResponse;
import com.unichain.pay.core.*;
import com.unichain.pay.entity.ChannelDirectiveRecord;
import com.unichain.pay.service.ChannelDirectiveRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("Mfe88BankcardPayDirective")
public class Mfe88BankcardPayDirective implements PayDirective<Mfe88ChannelDirecvite, BankcardPayParam> {
    @Autowired
    private ChannelDirectiveRecordService channelDirectiveRecordService;

    @Override
    public PayResponse exec(Mfe88ChannelDirecvite channelDirective, BankcardPayParam bankcardPayParam, PayRequest payRequest) {
        bankcardPayParam.setMerchantNo(channelDirective.getMerchantNo());

        String data = Mfe88PayRequestHandler.build(bankcardPayParam, channelDirective)
                /*.sign(channelDirective.getPrivateKey())
                .encrypt(channelDirective.getMerchantNo(), channelDirective.getSecretKey())*/
                .exec(channelDirective.getDirectiveUri());
        return record(bankcardPayParam, payRequest, data);

    }

    @Override
    public BankcardPayParam buildPayParam(PayRequest payRequest) {
        BankcardPayParam bankcardPayParam = new BankcardPayParam();
        Mfe88PayRequestHandler.buildPayParam(bankcardPayParam, payRequest);
        return bankcardPayParam;
    }

    @Override
    public Mfe88ChannelDirecvite buildChannelDirective() {
        return new Mfe88ChannelDirecvite();
    }

    @Override
    public String getDirectiveCode() {
        return "Mfe88BankcardPayDirective";
    }

    @Override
    public PayResponse record(BankcardPayParam bankcardPayParam, PayRequest payRequest, String data) {
        // 保存记录
        ChannelDirectiveRecord channelDirectiveRecord = new ChannelDirectiveRecord();

        channelDirectiveRecord.setAccountId(payRequest.getAccountId());
        channelDirectiveRecord.setAppId(payRequest.getAppId());
        channelDirectiveRecord.setChannelId(payRequest.getChannelId());
        channelDirectiveRecord.setUserId(payRequest.getUserId());
        channelDirectiveRecord.setCompanyId(payRequest.getCompanyId());
        channelDirectiveRecord.setDirectiveCode(getDirectiveCode());
        channelDirectiveRecord.setChannelId(payRequest.getChannelId());
        BankcardPrePayResponse bankcardPrePayResponse = JSONObject.parseObject(data, BankcardPrePayResponse.class);
        String returnCode = bankcardPrePayResponse.getDealCode();
        channelDirectiveRecord.setReturnCode(returnCode);
        channelDirectiveRecord.setPaymentId(bankcardPayParam.getOrderNo());
        channelDirectiveRecord.setReturnMsg(bankcardPrePayResponse.getDealMsg());
        channelDirectiveRecord.setBizCode(BizCode.PAY.val());
        PayResponse result = new PayResponse();

        PayResult payResult = new PayResult();
        //下单结果代码，为 10000 代表受理成功，否则受理失败，最终结果以代付查询为准详细参见 5.3 代码表
        if (!returnCode.equals("10000")) {
            // 0默认表示成功，1表示失败
            channelDirectiveRecord.setState(1);
            payResult.setStatus(PayResultStatusEnum.FAIL);
        } else {
            payResult.setStatus(PayResultStatusEnum.SUCCESS);
            payResult.setPaymentId(bankcardPayParam.getOrderNo());
        }
        Integer code = getResultCode(bankcardPrePayResponse);
        payResult.setCode(code);
        payResult.setResultMsg(bankcardPrePayResponse.getDealMsg());
        result.data(payResult);
        channelDirectiveRecordService.save(channelDirectiveRecord);
        return result;
    }

    public Integer getResultCode(BankcardPrePayResponse response) {
        Integer code = 0;
        if ("90001".equals(response.getDealCode())) {
            if ((response.getDealMsg().contains("账户可用余额不足") || response.getDealMsg().contains("账户余额不足"))) {
                code = PayStateCode.NOT_SUFFICIENT_FUNDS.code();
            }
            if ((response.getDealMsg().contains("上送的手机号或身份证号与已签约信息不一致") || response.getDealMsg().contains("请求中预留手机号与签约人在付款行预留的协议支付手机号不符"))) {
                code = PayStateCode.NOT_BANK_PHONE.code();
            }
            if (response.getDealMsg().contains("通道不支持的产品或设置不正确")) {
                code = PayStateCode.NOT_SUPPORT_BANKCARD.code();
            }
        } else if ("20029".equals(response.getDealCode()) && response.getDealMsg().equals("余额不足")) {
            code = PayStateCode.NOT_SUFFICIENT_FUNDS.code();
        }
        return code;
    }
}
