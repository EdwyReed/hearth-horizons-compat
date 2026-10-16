import * as T from 'three';
import {mergeGeometries} from 'three/addons/utils/BufferGeometryUtils.js';

export const PASSES=['blockout','structural-pass','form-refinement','material-pass','surface-pass','lighting-pass','interaction-pass','optimization-pass'];
export const PALETTE={
  wood:['#4A2E16','#6B4422','#8A5C2E','#B37D3F','#D6A85F'],
  iron:['#363636','#4B4B4B','#636363','#7F7F7F','#9B9B9B'],
  steel:['#555555','#737373','#939393','#B2B2B2','#D0D0D0'],
  brass:['#7A5A1F','#966E2A','#B8893B','#D1A84F','#E6C978']
};

// Image-matched reconstruction, model pixels (16 = one Minecraft block).
// No food, liquid, mixture plane or hidden lid exists in the bowl geometry.
export function createMixer({pass='optimization-pass'}={}){
  const stage=Math.max(0,PASSES.indexOf(pass)), detailed=stage>=1,form=stage>=2,pixel=stage>=3;
  const root=new T.Group();root.name='Manual_Countertop_Mixer';
  const parts={}, origins={};
  const material=new T.MeshStandardMaterial({vertexColors:true,roughness:.94,metalness:.05,flatShading:true});
  function part(name,pos=[0,0,0]){
    const g=new T.Group();g.name=name;g.position.set(...pos);g.userData={partId:name,selectable:true,collider:'box',pivot:[...pos]};
    root.add(g);parts[name]=g;origins[name]=new T.Vector3(...pos);return g;
  }
  const base=part('base'),column=part('column'),head=part('head'),bowl=part('bowl',[-2.7,1.45,0]);
  const gear=part('gear',[4.15,12.85,2.12]),crank=part('crank',[4.15,12.85,3.04]),whisk=part('whisk',[-2.7,10.95,0]);
  const trim=part('base-trim'),straps=part('column-straps'),bands=part('head-bands'),caps=part('top-caps'),rim=part('bowl-rim',[-2.7,1.45,0]),bolts=part('bolts');
  let serial=0;
  function paint(g,type,grain='y',seed=0){
    if(g.index)g=g.toNonIndexed();
    g.deleteAttribute('uv');
    const p=g.attributes.position,n=g.attributes.normal,cs=[];
    const bounds=new T.Box3().setFromBufferAttribute(p),sz=bounds.getSize(new T.Vector3());
    for(let q=0;q<p.count;q+=6){
      let x=0,y=0,z=0;const count=Math.min(6,p.count-q);
      for(let j=0;j<count;j++){x+=p.getX(q+j);y+=p.getY(q+j);z+=p.getZ(q+j);}x/=count;y/=count;z/=count;
      const ix=Math.floor((x-bounds.min.x)*2+.01),iy=Math.floor((y-bounds.min.y)*2+.01),iz=Math.floor((z-bounds.min.z)*2+.01);
      const hash=(Math.imul(ix+17,73856093)^Math.imul(iy+11,19349663)^Math.imul(iz+23,83492791)^seed)>>>0;
      let c=new T.Color(PALETTE[type][2]);
      if(pixel){
        if(type==='wood'){
          const across=grain==='x'?iy+iz:grain==='z'?ix+iy:ix+iz;
          const along=grain==='x'?ix:grain==='z'?iz:iy;
          // Long broad grain strokes plus restrained pixel flecks, not a checkerboard.
          const stripe=(Math.imul(across+seed+13,31)>>>0)%11;
          c.set(PALETTE.wood[stripe<2?1:stripe>8?3:2]);
          c.lerp(new T.Color(PALETTE.wood[2]),.35);
          c.multiplyScalar(.96+(hash%5)*.022+(Math.floor(along/5)%3)*.01);
        }else{
          c.set(PALETTE[type][2]);c.multiplyScalar(.87+(hash%7)*.035);
          // A narrow border within each flat plate gives the pixel-painted iron edge.
          const edge=(Math.abs(n.getZ(q))>.9&&(Math.abs(x-bounds.min.x)<.26||Math.abs(x-bounds.max.x)<.26||Math.abs(y-bounds.min.y)<.26||Math.abs(y-bounds.max.y)<.26));
          if(edge)c.lerp(new T.Color(PALETTE[type][3]),.5);
        }
      }
      if(stage>=4&&type!=='wood'&&hash%23===0)c.lerp(new T.Color(PALETTE[type][1]),.18);
      for(let j=0;j<count;j++)cs.push(c.r,c.g,c.b);
    }
    g.setAttribute('color',new T.Float32BufferAttribute(cs,3));return g;
  }
  function mesh(parent,g,type,name,grain='y'){
    const m=new T.Mesh(paint(g,type,grain,serial++),material);m.name=name||parent.name+'_'+serial;
    m.castShadow=true;m.receiveShadow=true;parent.add(m);return m;
  }
  function box(parent,size,pos,type='iron',name,grain='y',rz=0){
    const seg=size.map(s=>pixel?Math.max(1,Math.round(s*2)):1);
    const m=mesh(parent,new T.BoxGeometry(...size,...seg),type,name,grain);m.position.set(...pos);m.rotation.z=rz;return m;
  }
  function rod(parent,a,b,w=.35,d=.35,type='steel'){
    const av=new T.Vector3(...a),bv=new T.Vector3(...b),delta=bv.clone().sub(av);
    const m=box(parent,[w,delta.length(),d],av.clone().add(bv).multiplyScalar(.5).toArray(),type);
    m.quaternion.setFromUnitVectors(new T.Vector3(0,1,0),delta.normalize());return m;
  }
  box(base,[13.4,1.35,9],[0,.95,0],'wood','Timber_base','x');
  box(column,[2.65,11.55,3.2],[4.15,7.3,0],'wood','Timber_upright');
  box(head,[10.6,2.95,3.35],[-.1,13.05,0],'wood','Timber_cantilever','x');
  if(detailed){
    for(const z of [-4.25,4.25])box(trim,[13.9,.5,.55],[0,1.55,z],'iron','Base_edge_rail');
    for(const x of [-6.55,6.55])box(trim,[.55,.5,8.5],[x,1.55,0],'iron');
    for(const x of [-6.35,6.35])for(const z of [-4.05,4.05]){
      box(trim,[1.55,1.7,1.45],[x,.9,z],'iron','Corner_shoe');
      if(form)box(trim,[.65,.65,.08],[x,.95,z+Math.sign(z)*.75],'steel','Shoe_square_rivet');
    }
    // Column straps leave the broad wooden side panels exposed.
    for(const z of [-1.72,1.72]){
      box(straps,[2.35,11.6,.32],[4.15,7.6,z],'iron','Upright_iron_strap');
      for(const x of [3.08,5.22])box(straps,[.3,11.7,.4],[x,7.6,z],'steel','Strap_border');
    }
    box(straps,[3.35,4.45,3.7],[4.15,12.7,0],'iron','Gearbox_iron_band');
    // Retain wood across the middle of the far side of the gearbox.
    box(bands,[.85,3.15,3.65],[-5.25,13.05,0],'iron','Head_end_band');
    box(bands,[.3,2.8,3.8],[-5.7,13.05,0],'steel','End_band_border');
    box(bands,[1.85,.75,2.1],[-2.7,11.2,0],'iron','Spindle_socket');
    box(caps,[2.55,.85,3.05],[-3.85,14.94,0],'iron','Raised_spindle_cap');
    box(caps,[3.55,.6,3.85],[4.15,15.22,0],'iron','Raised_gearbox_cap');
    if(form){
      for(const x of [-3.85,4.15]){
        box(caps,[1,.12,1],[x,x<0?15.42:15.58,0],'steel','Cap_axle_square');
        box(caps,[.55,.13,.55],[x,x<0?15.49:15.65,0],'iron','Cap_recess');
      }
      for(const z of [-1.98,1.98])for(const y of [3.05,8.4])box(bolts,[.55,.55,.18],[4.15,y,z],'steel','Square_post_bolt');
    }
  }
  // Hollow bowl: 8 tapered wood staves + inner staves + wooden bottom.
  // Separate wall quads preserve a genuine cavity from every viewing angle.
  function facetedWall(parent,r0,r1,y0,y1,type,inner=false){
    const count=8,angle0=Math.PI/8;
    for(let i=0;i<count;i++){
      const a=i*Math.PI/4+angle0,b=a+Math.PI/4;
      const va=new T.Vector3(Math.cos(a),0,Math.sin(a)),vb=new T.Vector3(Math.cos(b),0,Math.sin(b));
      const segments=pixel?8:1,levels=pixel?Math.max(1,Math.round((y1-y0)*2)):1;
      const positions=[],normals=[];
      const normal=va.clone().add(vb).normalize().multiplyScalar(inner?-1:1);
      normal.y=(r0-r1)/(y1-y0)*(inner?-1:1);normal.normalize();
      for(let u=0;u<segments;u++)for(let v=0;v<levels;v++){
        const pts=[];
        for(const [du,dv] of [[0,0],[1,0],[1,1],[0,1]]){
          const t=(u+du)/segments,s=(v+dv)/levels,r=T.MathUtils.lerp(r0,r1,s);
          pts.push(va.clone().lerp(vb,t).multiplyScalar(r).setY(T.MathUtils.lerp(y0,y1,s)));
        }
        // Increasing angle then height has inward winding; flip for outer walls.
        const order=inner?[0,1,2,0,2,3]:[0,2,1,0,3,2];
        for(const k of order){positions.push(...pts[k].toArray());normals.push(...normal.toArray());}
      }
      const g=new T.BufferGeometry();g.setAttribute('position',new T.Float32BufferAttribute(positions,3));g.setAttribute('normal',new T.Float32BufferAttribute(normals,3));
      mesh(parent,g,type,(inner?'Inner_':'Outer_')+type+'_stave_'+i);
    }
  }
  function ring(parent,outer,inner,y,h,type){
    const a0=Math.PI/8;
    for(let i=0;i<8;i++){
      const a=a0+i*Math.PI/4,b=a+Math.PI/4;
      const pts=[new T.Vector2(Math.cos(a)*outer,Math.sin(a)*outer),new T.Vector2(Math.cos(b)*outer,Math.sin(b)*outer),new T.Vector2(Math.cos(b)*inner,Math.sin(b)*inner),new T.Vector2(Math.cos(a)*inner,Math.sin(a)*inner)];
      const shape=new T.Shape(pts);const g=new T.ExtrudeGeometry(shape,{depth:h,bevelEnabled:false,steps:1});g.rotateX(Math.PI/2);g.translate(0,y+h,0);
      mesh(parent,g,type,'Rim_segment_'+i);
    }
  }
  facetedWall(bowl,2.9,4.15,.2,4.35,'wood');
  facetedWall(bowl,2.55,3.7,.58,4.35,'wood',true);
  const bottom=new T.CylinderGeometry(2.64,2.64,.4,8,1,false,Math.PI/8);mesh(bowl,bottom,'wood','Empty_wooden_bottom').position.y=.4;
  ring(bowl,2.9,2.55,.2,.35,'wood');
  if(detailed){
    facetedWall(rim,4.24,4.24,4.17,4.8,'steel');
    facetedWall(rim,3.64,3.64,4.17,4.8,'steel',true);
    ring(rim,4.24,3.64,4.17,.63,'steel');
  }
  // Wheel lies on the column's front face. The hand crank shares its axle.
  const teeth=form?12:8;
  const disk=new T.CylinderGeometry(1.68,1.68,.55,teeth);disk.rotateX(Math.PI/2);mesh(gear,disk,'brass','Brass_cog_disk');
  for(let i=0;i<teeth;i++){
    const a=i*Math.PI*2/teeth;
    box(gear,[.68,.62,.65],[Math.cos(a)*1.77,Math.sin(a)*1.77,0],'brass','Cog_tooth_'+i,'y',a-Math.PI/2);
  }
  box(gear,[1.25,1.25,.78],[0,0,.38],'iron','Gear_hub');
  box(crank,[.9,.9,.5],[0,0,0],'steel','Crank_axle');
  box(crank,[.65,2.9,.65],[0,-1.15,.4],'iron','Crank_drop_arm');
  box(crank,[.6,.65,1.1],[0,-2.45,.8],'steel','Crank_elbow');
  // Finger grip projects OUT from the column, parallel to the main crank axle.
  box(crank,[.95,.95,3.15],[0,-2.45,2.575],'wood','Oak_finger_grip','z');
  if(form){box(crank,[1.02,1.02,.22],[0,-2.45,4.15],'iron','Grip_end_band');box(crank,[.5,.5,.2],[0,-2.45,4.3],'steel','Grip_end_rivet');}
  // Single beater centered under the head, as in the approved image.
  box(whisk,[.4,2.45,.4],[0,-1.15,0],'steel','Whisk_spindle');
  box(whisk,[1.15,.4,1.15],[0,-2.3,0],'iron','Whisk_crown');
  const width=.95,top=-2.35,low=-8;
  for(let i=0;i<4;i++){
    const a=i*Math.PI/2,c=Math.cos(a),s=Math.sin(a);
    rod(whisk,[.3*c,top,.3*s],[width*c,top-.4,width*s],.3,.3);
    rod(whisk,[width*c,top-.4,width*s],[width*c,low+.3,width*s],.32,.32);
    rod(whisk,[width*c,low+.3,width*s],[.28*c,low,.28*s],.3,.3);
  }
  // Group-local merges retain semantic parts and all moving pivots.
  if(stage>=7)for(const group of Object.values(parts)){
    const gs=[];for(const child of [...group.children]){
      child.updateMatrix();gs.push(child.geometry.clone().applyMatrix4(child.matrix));group.remove(child);
    }
    if(gs.length){const m=new T.Mesh(mergeGeometries(gs),material);m.name=group.name+'_mesh';m.castShadow=true;m.receiveShadow=true;group.add(m);}
  }
  root.userData={prototype:true,revision:4,reference:'manual-mixer-turnaround.png',bowlContents:'empty',pass,units:'model pixels',sculptRuntime:{pivots:['gear','crank','whisk'],sockets:{crankAxle:[4.15,12.85,3.04],spindle:[-2.7,10.95,0],bowlSeat:[-2.7,1.65,0]},colliders:Object.keys(parts).map(id=>({id,type:'box'}))}};
  function animate(phase){gear.rotation.z=-phase;crank.rotation.z=-phase;whisk.rotation.y=phase*4;}
  function explode(value){for(const [name,g] of Object.entries(parts)){const o=origins[name],center=new T.Vector3(0,7.5,0);g.position.copy(o).add(o.clone().sub(center).multiplyScalar(value*.35));}}
  function manifest(){return {model:root.name,parts:Object.values(parts).filter(g=>g.children.length).map(g=>({name:g.name,kind:'part',module:g.name,triangles:g.children.reduce((n,m)=>n+(m.geometry.index?m.geometry.index.count:m.geometry.attributes.position.count)/3,0)})),unnamedMeshes:0};}
  return {root,parts,bowl,rim,wheel:gear,crank,beaters:[whisk],materials:{},animate,explode,manifest};
}

export function animationClips(m){
  const times=Array.from({length:121},(_,i)=>i/60),tracks=[];
  for(const [object,axis,ratio] of [[m.wheel,new T.Vector3(0,0,1),-1],[m.crank,new T.Vector3(0,0,1),-1],[m.beaters[0],new T.Vector3(0,1,0),4]]){
    const values=times.flatMap(t=>new T.Quaternion().setFromAxisAngle(axis,t*Math.PI*ratio).toArray());
    tracks.push(new T.QuaternionKeyframeTrack(`${object.name}.quaternion`,times,values));
  }
  return [new T.AnimationClip('Hand_crank_and_whisk',2,tracks)];
}
