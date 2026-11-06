        label("거리",18,613);
        label("사업",105,613);
        label("광산",193,613);
        label("임무",281,613);
        label("Lv."+Math.max(1,tapLevel),63,560);
        label(fmt(coins),207,571);
        label(fmt(gems),207,544);
        label("초당 +"+fmt(income()),64,550);
        label("광물 "+fmt(ore)+"  영혼 "+fmt(souls),23,521);

        label(screen==1?"사업 운영":screen==2?"광산 탐험":screen==3?"성장 임무":"터치해서 돈 획득 +"+fmt(tapPower()),32,501);
        label(screen==1?"사업 제국":screen==2?"지하 "+depth+"층":screen==3?"임무 보상":rebirths>0?"환생 성장":"거리 노동",127,386);
        if(screen==0){
            label("터치 강화 "+tapLevel+" "+fmt(tapCost()),62,325);
            label("노동자 "+workerLevel+" "+fmt(workerCost()),62,267);
            label("광산 채굴 "+depth+"층",62,209);
            label("채굴 강화 "+drillLevel+" "+fmt(drillCost()),62,151);
            label("지하 확장 "+fmt(mineCost()),62,93);
            label("환생 100K "+rebirths+"회",62,37);
        } else if(screen==1){
            for(int i=0;i<5;i++)label(shopNames[i]+" "+shops[i]+" 비용 "+fmt(shopCost(i)),58,325-i*58);
            label("출석 보상 받기",58,37);
        } else if(screen==3){
            label("임무 보상 받기 "+(questTier+1),58,325);
            label("출석 보상 받기",58,267);
            label("터치 "+Math.max(0,totalTaps-questTapBase)+"/"+questTapTarget(),58,209);
            label("채굴 "+Math.max(0,mineActions-questMineBase)+"/"+questMineTarget(),58,151);
            label("완료 "+missions+"  보석 "+fmt(gems),58,93);
            label("거리로 돌아가기",58,37);
        } else {
            label("곡괭이 강화 "+drillLevel+" "+fmt(drillCost()),58,325);
            label("더 깊이 "+depth+"층 "+fmt(mineCost()),58,267);
            label("광물 전부 판매",58,209);
            label("보석 10개 교환",58,151);
            label("출석 보상",58,93);
            label("거리로 돌아가기",58,37);
        }
        for(int i=0;i<6;i++)label(screen==0&&i==5?"환생":"강화",287,new float[]{325,267,209,151,93,37}[i]);
        if(tapFloatTimer>0 && screen==0)label("+"+fmt(lastTapEarned),147,463+tapFloatTimer*32);
        label(notice,18,356);
        batch.end();
    }
    @Override public void pause(){persist();}
    @Override public void dispose(){persist();font.dispose();if(koreanAtlas!=null)koreanAtlas.dispose();streetSprite.dispose();if(streetArt!=null)streetArt.dispose();mineSprite.dispose();shopSprite.dispose();for(Texture t:actors)t.dispose();if(pngActor!=null)pngActor.dispose();batch.dispose();shapes.dispose();}
}
