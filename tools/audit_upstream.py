"""Source-derived Origins 1.20.1 schemas and independent codec corpus. Run from project root."""
import pathlib,re,json
PROJECT=pathlib.Path(__file__).resolve().parents[1]
OLD=PROJECT.parent/'.reference-apoli-2.9/src/main/java/io/github/apace100/apoli'
def args(s):
 out=[]; start=depth=0; quote=None; esc=False
 for i,c in enumerate(s):
  if quote:
   if esc:esc=False
   elif c=='\\':esc=True
   elif c==quote:quote=None
  elif c in '\"\'':quote=c
  elif c in '([{':depth+=1
  elif c in ')]}':depth-=1
  elif c==',' and depth==0:out.append(s[start:i].strip());start=i+1
 return out+[s[start:].strip()]
def enclosed(s,start):
 depth=0;quote=None;esc=False
 for i in range(start,len(s)):
  c=s[i]
  if quote:
   if esc:esc=False
   elif c=='\\':esc=True
   elif c==quote:quote=None
  elif c in '\"\'':quote=c
  elif c=='(':depth+=1
  elif c==')':
   depth-=1
   if depth==0:return s[start+1:i]
 raise ValueError('Unbalanced Java expression')
def fields(s):
 out={}
 for m in re.finditer(r'\.add\(\s*"([^"\n]+)"',s):
  a=args(enclosed(s,s.index('(',m.start())))
  out[m[1]]={'java_type':a[1],'required':len(a)==2}
  if len(a)>2:out[m[1]]['java_default']=a[2]
 return out
def group(p,kind):
 if kind=='Power':return 'POWER'
 for part in p.parts:
  if part in ('entity','item','block','bientity','damage','fluid','biome'):return part.upper().replace('BIENTITY','BI_ENTITY')+'_'+kind.upper()
 return p.stem.replace('Actions','').replace('Conditions','').replace('Client','').replace('Server','').upper().replace('BIENTITY','BI_ENTITY')+'_'+kind.upper()
def inventory():
 out={}
 for p in (OLD/'power').rglob('*.java'):
  s=p.read_text(encoding='utf-8');s=re.sub(r'"(?:[^"\\]|\\.)*"|//[^\n]*|/\*.*?\*/', lambda m: m[0] if m[0].startswith(chr(34)) else ' ', s, flags=re.S)
  for m in re.finditer(r'new (Power|Action|Condition)Factory(?:<[^\n]*?>)?\s*\(',s):
   a=args(enclosed(s,s.index('(',m.start())));id=re.search(r'Apoli.identifier\("([^"]+)"\)',a[0])
   if not id or len(a)<2:continue
   g=group(p.relative_to(OLD),m[1]);
   if g not in ['POWER','ENTITY_ACTION','BI_ENTITY_ACTION','BLOCK_ACTION','ITEM_ACTION','ENTITY_CONDITION','BI_ENTITY_CONDITION','BLOCK_CONDITION','ITEM_CONDITION','DAMAGE_CONDITION','FLUID_CONDITION','BIOME_CONDITION']:continue
   out.setdefault(g,{})[id[1]]={'source':p.relative_to(OLD).as_posix(),'fields':fields(a[1])}
 for id in re.findall(r'Apoli.identifier\("([^"]+)"\)',(OLD/'power/factory/PowerFactories.java').read_text()):
  out['POWER'].setdefault(id,{'source':'power/factory/PowerFactories.java','fields':{'modifier':{'java_type':'Modifier.DATA_TYPE','required':False},'modifiers':{'java_type':'Modifier.LIST_TYPE','required':False}} if id.startswith('modify_') else {}})
 out['POWER']['multiple']={'source':'power/PowerTypes.java','fields':{}}
 meta={'and':{'actions':'SELF_ACTIONS'},'chance':{'chance':'FLOAT','action':'SELF_ACTION'},'delay':{'ticks':'INT','action':'SELF_ACTION'},'if_else':{'condition':'SELF_CONDITION','if_action':'SELF_ACTION'},'if_else_list':{'actions':'BRANCHES'},'choice':{'actions':'CHOICES'},'nothing':{},'side':{'side':'SIDE','action':'SELF_ACTION'}}
 for g in ['ENTITY_ACTION','BI_ENTITY_ACTION','BLOCK_ACTION','ITEM_ACTION']:
  for id,fs in meta.items():out[g].setdefault(id,{'source':'power/factory/action (generic factory)','fields':{k:{'java_type':v,'required':True} for k,v in fs.items()}})
 return dict(sorted((g,dict(sorted(v.items()))) for g,v in out.items()))
def sample(n,t):
 if t=='BRANCHES':return [{'condition':{'type':'origins:constant','value':True},'action':{'type':'origins:nothing'}}]
 if t=='CHOICES':return [{'weight':1,'element':{'type':'origins:nothing'}}]
 if 'ACTION' in t:return [] if t.endswith('ACTIONS') else {'type':'origins:nothing'}
 if 'CONDITION' in t:return [] if t.endswith('CONDITIONS') else {'type':'origins:constant','value':True}
 if 'MODIFIER' in t.upper():
  mod={'operation':'addition','value':0.25}
  if 'ATTRIBUTED' in t:mod['attribute']='minecraft:generic.movement_speed'
  return [mod] if 'MODIFIERS' in t or 'LIST_TYPE' in t else mod
 if 'ITEM_STACK' in t:return {'item':'minecraft:stone'}
 if 'STATUS_EFFECT_INSTANCE' in t:return {'effect':'minecraft:speed','duration':100}
 fixed={'to':'water','from':'none','comparison':'>=','compare_to':1,'entity_type':'minecraft:snowball','attribute':'minecraft:generic.movement_speed','equipment_slot':'mainhand','power':'bridge_test:resource','resource':'bridge_test:resource','power_type':'bridge_test:resource','source':'bridge_test:source','fluid':'minecraft:water','dimension':'minecraft:overworld','sound':'minecraft:entity.player.levelup','enchantment':'minecraft:protection','event':'minecraft:step','group':'default','ingredient':{'item':'minecraft:stone'},'block':'minecraft:stone','tag':'minecraft:water' if 'FLUID' in t else 'minecraft:is_forest','property':'age','name':'generic','side':'server','category':'forest','texture':'minecraft:textures/misc/underwater.png','shader':'minecraft:shaders/post/creeper.json','draw_mode':'texture','draw_phase':'above_hud','ability':'minecraft:allow_flying','precipitation':'rain','gamemode':'survival','predicate':'bridge_test:predicate','command':'say bridge-test','particle':'minecraft:cloud','nbt':'{}','modifier':'minecraft:empty','recipe':{'id':'bridge_test:recipe','type':'minecraft:crafting_shapeless','ingredients':[{'item':'minecraft:dirt'}],'result':{'item':'minecraft:stone'}},'render_elytra':True,'allow':True,'class':'modify_jump'}
 if n=='power' and 'FLOAT' in t:return 2.0
 if n in fixed:return fixed[n]
 if 'BOOLEAN' in t:return True
 if 'NBT' in t:return '{}'
 if any(x in t for x in ['INT','FLOAT','DOUBLE']):return 1
 return 'minecraft:stone'
if __name__=='__main__':
 schemas=inventory(); (PROJECT/'src/main/resources/legacy-1.20.1-schemas.json').write_text(json.dumps(schemas,indent=2)+'\n')
 corpus=[]
 for g,specs in schemas.items():
  for id,spec in specs.items():
   data={'type':'origins:'+id}
   for n,f in spec['fields'].items():
    if f['required']:data[n]=sample(n,f['java_type'])
   corpus.append({'name':g.lower()+'/'+id,'context':g,'data':data})
 additional=[]
 for case in corpus:
  alt=json.loads(json.dumps(case)); alt['name']+='@apoli'; alt['data']['type']=alt['data']['type'].replace('origins:', 'apoli:',1); additional.append(alt)
 corpus.extend(additional)
 (PROJECT/'tests/upstream-codec-corpus.json').write_text(json.dumps(corpus,indent=2)+'\n')
 print({g:len(s) for g,s in schemas.items()});print(len(corpus),'source-derived codec cases')
