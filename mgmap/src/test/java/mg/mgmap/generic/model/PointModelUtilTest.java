package mg.mgmap.generic.model;

import org.junit.Test;
import static org.junit.Assert.*;

public class PointModelUtilTest {

    @Test
    public void distance() {
        MultiPointModelImpl mpm = new MultiPointModelImpl();
        mpm.addPoint(new PointModelImpl(49.4001, 8.6001));
        mpm.addPoint(new PointModelImpl(49.4003, 8.6001));
        mpm.addPoint(new PointModelImpl(49.4004, 8.6001));
        double dist = PointModelUtil.distance(mpm);
        assertTrue( (Math.abs( dist )-33.394) < 0.01);
    }

    @Test
    public void findApproach1() {
        PointModelUtil.init(32);
        PointModel pm1 = new PointModelImpl(49.4001, 8.6001);
        PointModel pm2 = new PointModelImpl(49.4003, 8.6001);
        PointModel pm = new PointModelImpl(49.4002, 8.6002);
        WriteablePointModelImpl pmRes = new WriteablePointModelImpl();
        assertTrue(PointModelUtil.findApproach(pm, pm1, pm2, pmRes));
        assertEquals(49.4002, pmRes.getLat(),0);
    }

    @Test
    public void findApproach2() {
        PointModelUtil.init(32);
        PointModel pm1 = new PointModelImpl(49.4001, 8.6001);
        PointModel pm2 = new PointModelImpl(49.4001, 8.6001);
        PointModel pm = new PointModelImpl(49.4002, 8.6002);
        WriteablePointModelImpl pmRes = new WriteablePointModelImpl();
        assertFalse(PointModelUtil.findApproach(pm, pm1, pm2, pmRes));
    }

    @Test
    public void interpolate() {
        PointModel pm1 = new PointModelImpl(49.4001, 8.6001);
        PointModel pm2 = new PointModelImpl(49.4003, 8.6001);
        PointModel pm = PointModelUtil.interpolate(pm1, pm2, 11.132);
        assertEquals(49.4002, pm.getLat(),0);
        assertEquals(8.6001, pm.getLon(),0);
    }

    @Test
    public void calcDegree1(){
        PointModel pm1 = new PointModelImpl(49.4001, 8.6001);
        PointModel pm2 = new PointModelImpl(49.4003, 8.6001);
        PointModel pm3 = new PointModelImpl(49.4003, 8.6002);
        assertEquals( 270,PointModelUtil.calcDegree(pm1,pm2,pm3), 0.01);
    }
    @Test
    public void calcDegree2(){
        PointModel pm1 = new PointModelImpl(49.4001, 8.6001);
        PointModel pm2 = new PointModelImpl(49.4003, 8.6001);
        PointModel pm3 = new PointModelImpl(49.4003, 8.6000);
        assertEquals( 90,PointModelUtil.calcDegree(pm1,pm2,pm3), 0.01);
    }
    @Test
    public void calcDegree3(){
        PointModel pm1 = new PointModelImpl(49.4001, 8.6001);
        PointModel pm2 = new PointModelImpl(49.4003, 8.6001);
        PointModel pm3 = new PointModelImpl(49.4003, 8.6001);
        assertEquals( -1,PointModelUtil.calcDegree(pm1,pm2,pm3),0);
    }

    @Test
    public void clock4Degree(){
       assertEquals( -1,PointModelUtil.clock4degree(-2),0);
        assertEquals( 12,PointModelUtil.clock4degree(180),0);
        assertEquals( 3,PointModelUtil.clock4degree(270),0);
        assertEquals( 9,PointModelUtil.clock4degree(90),0);
        assertEquals( -1,PointModelUtil.clock4degree(361),0);
        assertEquals( 8,PointModelUtil.clock4degree(45),0);
        assertEquals( 7,PointModelUtil.clock4degree(44.99),0);
    }

    @Test
    public void turnLeft1(){
        PointModel pm1 = new PointModelImpl(49.4001, 8.6001);
        PointModel pm2 = new PointModelImpl(49.4003, 8.6001);
        PointModel pm3 = new PointModelImpl(49.4003, 8.6000);
        assertTrue( PointModelUtil.turnLeft(pm1,pm2,pm3));
    }
    @Test
    public void turnLeft2(){
        PointModel pm1 = new PointModelImpl(49.4001, 8.6001);
        PointModel pm2 = new PointModelImpl(49.4003, 8.6001);
        PointModel pm3 = new PointModelImpl(49.4003, 8.6002);
        assertFalse( PointModelUtil.turnLeft(pm1,pm2,pm3));
    }
    @Test
    public void turnLeft3(){
        PointModel pm1 = new PointModelImpl(49.4001, 8.6001);
        PointModel pm2 = new PointModelImpl(49.4001, 8.6003);
        PointModel pm3 = new PointModelImpl(49.4003, 8.6000);
        assertTrue( PointModelUtil.turnLeft(pm1,pm2,pm3));
    }
    @Test
    public void turnLeft4(){
        PointModel pm1 = new PointModelImpl(49.4001, 8.6001);
        PointModel pm2 = new PointModelImpl(49.4003, 8.6003);
        PointModel pm3 = new PointModelImpl(49.4000, 8.6003);
        assertFalse( PointModelUtil.turnLeft(pm1,pm2,pm3));
    }

}